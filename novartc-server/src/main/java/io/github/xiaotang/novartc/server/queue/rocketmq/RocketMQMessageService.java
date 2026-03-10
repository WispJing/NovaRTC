package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.enums.RTCTopic;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.MessageService;
import io.github.xiaotang.novartc.server.queue.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created on 2026/3/10 13:50
 *
 * @author tangj
 * @description
 */
@Service
@ConditionalOnBean(name = "rocketMQTemplate")
@RequiredArgsConstructor
public class RocketMQMessageService implements MessageService {
    private final RocketMQTemplate rocketMQTemplate;
    private final UserRouteService userRouteService;
    private final UserChannelManager userChannelManager;
    private final InstanceIdProvider instanceIdProvider;

    @Override
    public void sendMessage(ChatMessage.MsgBody message) {
        Set<String> targetUserIds = resolveTargetUserIds(message);
        if (targetUserIds.isEmpty()) {
            return;
        }

        // 按实例分组：实例ID -> 该实例上的用户ID列表
        Map<String, Set<String>> instanceToUsers = new HashMap<>();
        for (String userId : targetUserIds) {
            Set<String> instances = userRouteService.getUserInstances(userId);
            if (instances == null || instances.isEmpty()) {
                continue;
            }
            for (String inst : instances) {
                instanceToUsers.computeIfAbsent(inst, k -> new HashSet<>()).add(userId);
            }
        }

        // 对本实例的用户直接本地发送，其他实例通过RocketMQ发送
        String localInstance = instanceIdProvider.getInstanceId();
        for (Map.Entry<String, Set<String>> entry : instanceToUsers.entrySet()) {
            String targetInstance = entry.getKey();
            Set<String> users = entry.getValue();
            if (targetInstance.equals(localInstance)) {
                // 本地发送
                users.forEach(userId -> userChannelManager.sendToUser(userId, message));
            } else {
                // 构建RocketMQ消息，Tag为目标实例ID，消息体包含原始消息和接收者列表
                Map<String, Object> payload = new HashMap<>();
                payload.put("msgBody", message.toByteArray()); // 使用protobuf字节
                payload.put("targetUserIds", users);
                // 发送到RocketMQ
                rocketMQTemplate.send(
                        message.getIsGroup() ? RTCTopic.GROUP_MSG.getTopic() : RTCTopic.PRIVATE_MSG.getTopic()
                                + ":" + targetInstance,
                        MessageBuilder
                                .withPayload(payload)
                                .setHeader("msgId", message.getMessageId())
                                .build()
                );
            }
        }
    }

    // 解析消息的目标用户ID列表
    private Set<String> resolveTargetUserIds(ChatMessage.MsgBody message) {
        Set<String> userIds = new HashSet<>();
        if (message.getIsGroup()) {
            String groupId = message.getGroupChatInfo().getGroupId();
            userIds.addAll(userChannelManager.getGroupMembers(groupId));
        } else {
            userIds.add(message.getSingleChatInfo().getUser2Id());
        }
        return userIds;
    }
}
