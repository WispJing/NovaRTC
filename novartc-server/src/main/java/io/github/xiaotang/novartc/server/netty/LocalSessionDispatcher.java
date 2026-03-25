package io.github.xiaotang.novartc.server.netty;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 本地连接与群成员缓存，只负责本机内存中的会话读写和消息投递。
 */
@Component
public class LocalSessionDispatcher {
    private final Map<String, Set<Channel>> userChannels = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();
    private final Map<String, Channel> tokenChannels = new ConcurrentHashMap<>();

    public void bindChannel(String userId, String token, Channel channel) {
        channel.attr(NettyAttributes.USER_ID).set(userId);
        channel.attr(NettyAttributes.TOKEN).set(token);
        userChannels.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(channel);
        tokenChannels.put(token, channel);
    }

    public SessionBinding removeChannel(Channel channel) {
        if (channel == null) {
            return new SessionBinding(null, null, false);
        }
        String userId = channel.attr(NettyAttributes.USER_ID).get();
        String token = channel.attr(NettyAttributes.TOKEN).get();
        if (token != null) {
            tokenChannels.remove(token);
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
                .forEach(channel -> channel.writeAndFlush(msg));
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
