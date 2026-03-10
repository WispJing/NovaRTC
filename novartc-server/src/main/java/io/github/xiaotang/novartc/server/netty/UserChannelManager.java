package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    @Autowired
    private UserRouteService userRouteService;
    @Autowired
    private InstanceIdProvider instanceIdProvider;

    // 绑定用户Channel（用户上线）
    public void bind(String userId, Channel channel) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        channel.attr(NettyAttributes.USER_ID).set(userId);
        userChannels.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(channel);
        // 向Redis注册该用户在本实例
        userRouteService.registerUser(userId, instanceIdProvider.getInstanceId());
        ChatAccessLog.log.info("User {} bound, channel: {}", userId, channel.id());
    }

    // 移除用户Channel（用户下线）
    public void remove(Channel channel) {
        String userId = channel.attr(NettyAttributes.USER_ID).get();
        if (StringUtils.isEmpty(userId)) {
            return;
        }
        Set<Channel> channels = userChannels.get(userId);
        if (channels != null) {
            channels.remove(channel);
            if (channels.isEmpty()) {
                userChannels.remove(userId);
                // Redis中注销该实例
                userRouteService.unregisterUser(userId, instanceIdProvider.getInstanceId());
                ChatAccessLog.log.info("User {} fully removed", userId);
            }
        }
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

    // 获取用户在本地的所有Channel
    public Set<Channel> getLocalChannels(String userId) {
        return userChannels.get(userId);
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
}
