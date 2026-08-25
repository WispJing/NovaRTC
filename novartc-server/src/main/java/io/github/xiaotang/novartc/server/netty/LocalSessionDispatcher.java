package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 本地连接与群成员缓存，只负责本机内存中的会话读写和消息投递。
 */
@Component
public class LocalSessionDispatcher {
    private final Map<String, Set<Channel>> userChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();
    private final Map<String, Channel> tokenChannels = new ConcurrentHashMap<>();
    /**
     * 当前已认证连接数，用于连接上限保护与监控指标
     */
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    public void bindChannel(String userId, String token, Channel channel) {
        channel.attr(NettyAttributes.USER_ID).set(userId);
        channel.attr(NettyAttributes.TOKEN).set(token);
        userChannels.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(channel);
        tokenChannels.put(token, channel);
        connectionCount.incrementAndGet();
    }

    /**
     * 获取当前已认证连接数。
     */
    public int getConnectionCount() {
        return connectionCount.get();
    }

    public SessionBinding removeChannel(Channel channel) {
        if (channel == null) {
            return new SessionBinding(null, null, false);
        }
        String userId = channel.attr(NettyAttributes.USER_ID).get();
        String token = channel.attr(NettyAttributes.TOKEN).get();
        if (token != null && tokenChannels.remove(token) != null) {
            connectionCount.decrementAndGet();
        }
        boolean userOffline = false;
        if (userId != null) {
            Set<Channel> channels = userChannels.get(userId);
            if (channels != null) {
                channels.remove(channel);
                if (channels.isEmpty()) {
                    userChannels.remove(userId);
                    userOffline = true;
                }
            }
        }
        return new SessionBinding(userId, token, userOffline);
    }

    public Channel getChannelByToken(String token) {
        return tokenChannels.get(token);
    }

    public void closeByToken(String token) {
        Channel channel = tokenChannels.get(token);
        if (channel != null && channel.isActive()) {
            channel.close();
        }
    }

    public Set<String> getTokensByUserId(String userId) {
        Set<Channel> channels = userChannels.get(userId);
        if (channels == null) {
            return Set.of();
        }
        return channels.stream()
                .map(channel -> channel.attr(NettyAttributes.TOKEN).get())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void sendToUser(String userId, Object msg) {
        Set<Channel> channels = userChannels.get(userId);
        if (channels == null || channels.isEmpty()) {
            return;
        }
        channels.stream()
                .filter(Channel::isActive)
                .forEach(channel -> {
                    // 背压保护：写缓冲拥塞说明客户端消费过慢，直接断开防止缓冲无限堆积
                    if (!channel.isWritable()) {
                        ChatAccessLog.log.warn("通道写缓冲拥塞，关闭慢速客户端保护服务端: userId={}, channelId={}",
                                userId, channel.id());
                        channel.close();
                        return;
                    }
                    channel.writeAndFlush(msg);
                });
    }

    public boolean isLocalChannel(String userId) {
        return !CollectionUtils.isEmpty(userChannels.get(userId));
    }

    public Set<Channel> getLocalChannels(String userId) {
        return userChannels.get(userId);
    }

    public void sendToAllUsers(Object msg) {
        userChannels.values().stream()
                .flatMap(Set::stream)
                .filter(Channel::isActive)
                .forEach(channel -> channel.writeAndFlush(msg));
    }

    /**
     * 获取当前所有有连接的用戶 ID 快照，用于优雅关停时摘除路由。
     */
    public Set<String> getConnectedUserIds() {
        return Set.copyOf(userChannels.keySet());
    }

    /**
     * 向所有在线连接下发系统通知并在写完后关闭，用于优雅迁移。
     */
    public void closeAllWithNotice(String notice) {
        ChatMessage.MsgBody noticeMessage = ChatMessage.MsgBody.newBuilder()
                .setMessageType(ChatMessage.MessageType.SYSTEM)
                .setTimestamp(System.currentTimeMillis())
                .setTextContent(ChatMessage.TextContent.newBuilder().setText(notice).build())
                .build();
        userChannels.values().stream()
                .flatMap(Set::stream)
                .filter(Channel::isActive)
                .forEach(channel -> channel.writeAndFlush(noticeMessage)
                        .addListener(ChannelFutureListener.CLOSE));
    }

    public void sendToUsers(Set<String> userIds, Object msg) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userIds.forEach(userId -> sendToUser(userId, msg));
    }

    public void addGroupMember(String groupId, String userId) {
        groupMembers.computeIfAbsent(groupId, key -> ConcurrentHashMap.newKeySet()).add(userId);
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

    public record SessionBinding(String userId, String token, boolean userOffline) {
    }
}
