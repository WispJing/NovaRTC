package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created on 2026/3/5 14:04
 *
 * @author tangj
 * @description
 */
@Component
public class UserChannelManager {
    // 用户ID -> 该用户在本实例的所有Channel
    private final Map<String, Set<Channel>> userChannels = new ConcurrentHashMap<>();
    // 群组ID -> 群组成员ID集合（缓存）
    private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();
    // token -> Channel（一个token对应一个连接）
    private final Map<String, Channel> tokenChannels = new ConcurrentHashMap<>();

    @Autowired
    private UserRouteService userRouteService;
    @Autowired
    private InstanceIdProvider instanceIdProvider;

    // 绑定用户Channel（用户上线）
    public void bind(String userId, String token, Channel channel) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(token)) {
            return;
        }
        boolean wasOffline = !isOnline(userId);
        channel.attr(NettyAttributes.USER_ID).set(userId);
        channel.attr(NettyAttributes.TOKEN).set(token);
        userChannels.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(channel);
        tokenChannels.put(token, channel);
        userRouteService.registerUser(userId, instanceIdProvider.getInstanceId());
        if (wasOffline) {
            broadcastPresence(userId, ChatMessage.PresenceStatus.ONLINE, "在线");
        }
        // 可选：记录 token 与实例的映射，用于精确路由
        // userRouteService.registerToken(token, instanceIdProvider.getInstanceId());
        ChatAccessLog.log.info("User {} bound with token {}, channel: {}", userId, token, channel.id());
    }

    // 移除用户Channel（用户下线）
    public void remove(Channel channel) {
        if (channel == null) {
            return;
        }
        String userId = channel.attr(NettyAttributes.USER_ID).get();
        String token = channel.attr(NettyAttributes.TOKEN).get();
        if (StringUtils.isNotEmpty(token)) {
            tokenChannels.remove(token);
            // 可选：删除 token 与实例的映射
            // userRouteService.unregisterToken(token, instanceIdProvider.getInstanceId());
        }
        if (StringUtils.isNotEmpty(userId)) {
            Set<Channel> channels = userChannels.get(userId);
            if (channels != null) {
                channels.remove(channel);
                if (channels.isEmpty()) {
                    userChannels.remove(userId);
                    userRouteService.unregisterUser(userId, instanceIdProvider.getInstanceId());
                    if (!isOnline(userId)) {
                        broadcastPresence(userId, ChatMessage.PresenceStatus.OFFLINE, "离线");
                    }
                }
            }
        }
        if (channel.isActive()) {
            channel.close();
        }
        ChatAccessLog.log.info("Channel {} removed", channel.id());
    }

    // 新增方法：根据 token 获取 Channel
    public Channel getChannelByToken(String token) {
        return tokenChannels.get(token);
    }

    // 新增方法：关闭指定 token 的连接
    public void closeByToken(String token) {
        remove(tokenChannels.get(token));
    }

    // 根据 userId 获取所有 token
    public Set<String> getTokensByUserId(String userId) {
        Set<Channel> channels = userChannels.get(userId);
        if (channels == null) return Set.of();
        return channels.stream()
                .map(ch -> ch.attr(NettyAttributes.TOKEN).get())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    // 向指定用户发送消息（本地发送）
    public void sendToUser(String userId, Object msg) {
        Set<Channel> channels = userChannels.get(userId);
        if (channels == null || channels.isEmpty()) {
            return;
        }
        channels.stream()
                .filter(Channel::isActive)
                .forEach(ch -> ch.writeAndFlush(msg));
    }

    public Boolean isLocalChannel(String userId) {
        return !CollectionUtils.isEmpty(userChannels.get(userId));
    }

    public boolean isOnline(String userId) {
        return StringUtils.isNotBlank(userId) && !userRouteService.getUserInstances(userId).isEmpty();
    }

    // 获取用户在本地的所有Channel
    public Set<Channel> getLocalChannels(String userId) {
        return userChannels.get(userId);
    }

    public void sendToAllUsers(Object msg) {
        userChannels.values().stream()
                .flatMap(Set::stream)
                .filter(Channel::isActive)
                .forEach(channel -> channel.writeAndFlush(msg));
    }

    // 群组缓存操作
    public void addGroupMember(String groupId, String userId) {
        groupMembers.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void removeGroupMember(String groupId, String userId) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.remove(userId);
            if (members.isEmpty()) {
                groupMembers.remove(groupId);
            }
        }
    }

    public Set<String> getGroupMembers(String groupId) {
        return groupMembers.getOrDefault(groupId, Set.of());
    }

    private void broadcastPresence(String userId, ChatMessage.PresenceStatus status, String message) {
        ChatMessage.MsgBody presenceMessage = ChatMessage.MsgBody.newBuilder()
                .setMessageId("presence-" + userId + "-" + System.currentTimeMillis())
                .setMessageType(ChatMessage.MessageType.PRESENCE)
                .setTimestamp(System.currentTimeMillis())
                .setPresenceMessage(ChatMessage.Presence.newBuilder()
                        .setUserId(userId)
                        .setStatus(status)
                        .setLastActiveAt(System.currentTimeMillis())
                        .setIsBroadcast(true)
                        .setMessage(message == null ? "" : message)
                        .build())
                .build();
        sendToAllUsers(presenceMessage);
    }
}
