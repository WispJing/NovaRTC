package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.netty.LocalSessionDispatcher;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.RocketMQHeader;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RocketMQ 会话消息分发服务（多实例模式）。
 * <p>
 * 按目标用户所在实例精确路由：以实例 ID 作为消息 Tag，Broker 侧完成过滤，
 * 各实例只拉取发给自己的消息，避免全量广播放大；路由表未命中的用户
 * 走 {@link RocketMQHeader#FALLBACK_TAG} 兜底，所有实例接收后按本地连接过滤投递。
 */
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RocketMQChatDispatchService implements ChatDispatchService {
    private final Logger logger = LoggerFactory.getLogger(RocketMQChatDispatchService.class);
    private final RocketMQTemplate rocketMQTemplate;
    private final UserRouteService userRouteService;
    private final LocalSessionDispatcher localSessionDispatcher;
    private final InstanceIdProvider instanceIdProvider;
    private final IMConfigProperties iMConfigProperties;

    @Override
    public void dispatch(ChatMessage.MsgBody message) {
        Set<String> targetUserIds = resolveTargetUserIds(message);
        if (targetUserIds.isEmpty()) {
            return;
        }

        // 本实例在线用户直接投递，其余用户按所在实例分组经 MQ 精确路由
        Set<String> remoteUserIds = new HashSet<>();
        for (String userId : targetUserIds) {
            if (localSessionDispatcher.isLocalChannel(userId)) {
                localSessionDispatcher.sendToUser(userId, message);
            } else {
                remoteUserIds.add(userId);
            }
        }
        if (remoteUserIds.isEmpty()) {
            return;
        }

        groupTargetsByInstance(remoteUserIds)
                .forEach((tag, userIds) -> sendToInstance(tag, message, userIds));
    }

    /**
     * 按目标实例对用户分组。
     * <p>
     * 路由表为空（或只剩本地实例的过期路由）的用户归入 {@link RocketMQHeader#FALLBACK_TAG} 组，
     * 由各实例消费后按本地连接过滤，保证消息必达。
     *
     * @param userIds 非本实例在线的目标用户集合
     * @return 实例 ID（或 fallback 标签）到用户集合的映射
     */
    Map<String, Set<String>> groupTargetsByInstance(Set<String> userIds) {
        String localInstance = instanceIdProvider.getInstanceId();
        Map<String, Set<String>> instanceToUsers = new HashMap<>();
        for (String userId : userIds) {
            Set<String> instances = userRouteService.getUserInstances(userId);
            Set<String> remoteInstances = instances == null ? Set.of()
                    : instances.stream()
                    .filter(StringUtils::isNotBlank)
                    .filter(instanceId -> !StringUtils.equals(instanceId, localInstance))
                    .collect(Collectors.toSet());
            if (remoteInstances.isEmpty()) {
                instanceToUsers.computeIfAbsent(RocketMQHeader.FALLBACK_TAG, key -> new HashSet<>()).add(userId);
                continue;
            }
            for (String instanceId : remoteInstances) {
                instanceToUsers.computeIfAbsent(instanceId, key -> new HashSet<>()).add(userId);
            }
        }
        return instanceToUsers;
    }

    private void sendToInstance(String tag, ChatMessage.MsgBody message, Set<String> userIds) {
        try {
            SendResult sendResult = rocketMQTemplate.syncSendOrderly(
                    iMConfigProperties.getChatTopic() + ":" + tag,
                    MessageBuilder
                            .withPayload(message.toByteArray())
                            .setHeader(RocketMQHeader.MSG_ID, message.getMessageId())
                            .setHeader(RocketMQHeader.TARGET_USER_ID, String.join(",", userIds))
                            .build(),
                    message.getSender().getUserId()
            );
            logger.info("RocketMQ 会话消息投递结果: tag={}, users={}, {}", tag, userIds, JacksonUtil.toJson(sendResult));
        } catch (Exception e) {
            logger.error("RocketMQ 会话消息投递失败: tag={}, users={}", tag, userIds, e);
        }
    }

    // 解析消息的目标用户ID列表
    private Set<String> resolveTargetUserIds(ChatMessage.MsgBody message) {
        Set<String> userIds = new HashSet<>();
        if (message.getIsGroup()) {
            String groupId = message.getGroupChatInfo().getGroupId();
            userIds.addAll(localSessionDispatcher.getGroupMembers(groupId));
        } else {
            userIds.add(message.getSingleChatInfo().getUser2Id());
        }
        return userIds.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }
}
