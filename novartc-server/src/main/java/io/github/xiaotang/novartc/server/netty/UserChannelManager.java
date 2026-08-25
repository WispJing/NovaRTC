package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.PresenceDispatchService;
import io.github.xiaotang.novartc.server.service.PresenceAudienceResolver;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 负责用户连接生命周期和在线状态协调。
 */
@Component
@RequiredArgsConstructor
public class UserChannelManager {
    private final UserRouteService userRouteService;
    private final InstanceIdProvider instanceIdProvider;
    private final PresenceDispatchService presenceDispatchService;
    private final LocalSessionDispatcher localSessionDispatcher;
    private final PresenceAudienceResolver presenceAudienceResolver;
    /**
     * 在线状态分发异步线程池，避免受众解析与 MQ 同步发送阻塞 Netty IO 线程
     */
    @Qualifier("imPresenceExecutor")
    private final Executor imPresenceExecutor;
    private final IMConfigProperties imConfigProperties;

    /**
     * 判断单实例连接数是否已达上限（配置为 0 或空表示不限制）。
     */
    public boolean isConnectionLimitReached() {
        Long maxConnections = imConfigProperties.getMaxConnections();
        return maxConnections != null && maxConnections > 0
                && localSessionDispatcher.getConnectionCount() >= maxConnections;
    }

    public void bind(String userId, String token, Channel channel) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(token)) {
            return;
        }
        boolean wasOffline = !isOnline(userId);
        localSessionDispatcher.bindChannel(userId, token, channel);
        userRouteService.registerUser(userId, instanceIdProvider.getInstanceId());
        if (wasOffline) {
            imPresenceExecutor.execute(() -> dispatchPresence(userId, ChatMessage.PresenceStatus.ONLINE, "在线"));
        }
        ChatAccessLog.log.info("用户连接绑定完成: userId={}, token={}, channelId={}", userId, token, channel.id());
    }

    public void remove(Channel channel) {
        if (channel == null) {
            return;
        }
        LocalSessionDispatcher.SessionBinding binding = localSessionDispatcher.removeChannel(channel);
        String userId = binding.userId();
        if (StringUtils.isNotEmpty(userId) && binding.userOffline()) {
            userRouteService.unregisterUser(userId, instanceIdProvider.getInstanceId());
            if (!isOnline(userId)) {
                imPresenceExecutor.execute(() -> dispatchPresence(userId, ChatMessage.PresenceStatus.OFFLINE, "离线"));
            }
        }
        if (channel.isActive()) {
            channel.close();
        }
        ChatAccessLog.log.info("连接已移除: channelId={}", channel.id());
    }

    public boolean isOnline(String userId) {
        return StringUtils.isNotBlank(userId) && !userRouteService.getUserInstances(userId).isEmpty();
    }

    public boolean isLocalChannel(String userId) {
        return localSessionDispatcher.isLocalChannel(userId);
    }

    public void sendToUser(String userId, Object msg) {
        localSessionDispatcher.sendToUser(userId, msg);
    }

    public void sendToAllUsers(Object msg) {
        localSessionDispatcher.sendToAllUsers(msg);
    }

    public void closeByToken(String token) {
        localSessionDispatcher.closeByToken(token);
    }

    public Set<String> getGroupMembers(String groupId) {
        return localSessionDispatcher.getGroupMembers(groupId);
    }

    public void addGroupMember(String groupId, String userId) {
        localSessionDispatcher.addGroupMember(groupId, userId);
    }

    public void removeGroupMember(String groupId, String userId) {
        localSessionDispatcher.removeGroupMember(groupId, userId);
    }

    public Channel getChannelByToken(String token) {
        return localSessionDispatcher.getChannelByToken(token);
    }

    public Set<String> getTokensByUserId(String userId) {
        return localSessionDispatcher.getTokensByUserId(userId);
    }

    public Set<Channel> getLocalChannels(String userId) {
        return localSessionDispatcher.getLocalChannels(userId);
    }

    private void dispatchPresence(String userId, ChatMessage.PresenceStatus status, String message) {
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
        Set<String> audienceUserIds = presenceAudienceResolver.resolveAudienceUserIds(userId);
        if (audienceUserIds.isEmpty()) {
            return;
        }
        presenceDispatchService.dispatch(presenceMessage, audienceUserIds);
    }
}
