package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.MessageService;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.RocketMQHeader;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Created on 2026/3/10 13:50
 *
 * @author tangj
 * @description
 */
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RocketMQMessageService implements MessageService {
    private final Logger logger = LoggerFactory.getLogger(RocketMQMessageService.class);
    private final RocketMQTemplate rocketMQTemplate;
    private final UserRouteService userRouteService;
    private final UserChannelManager userChannelManager;
    private final InstanceIdProvider instanceIdProvider;
    private final IMConfigProperties iMConfigProperties;

    @Override
    public void sendMessage(ChatMessage.MsgBody message) {
        Set<String> targetUserIds = resolveTargetUserIds(message);
        if (targetUserIds.isEmpty()) {
            return;
        }

        Set<String> mqToUsers = new HashSet<>();
        for (String userId : targetUserIds) {
            /*if (userChannelManager.isLocalChannel(userId)) {
                userChannelManager.sendToUser(userId, message);
                continue;
            }*/
            mqToUsers.add(userId);
        }
        if (!CollectionUtils.isEmpty(mqToUsers)) {
            try {
                rocketMQTemplate.syncSendOrderly(
                        iMConfigProperties.getImMsgTopic(),
                        MessageBuilder
                                .withPayload(message.toByteArray())
                                .setHeader(RocketMQHeader.MSG_ID, message.getMessageId())
                                .setHeader(RocketMQHeader.TARGET_USER_ID, String.join(",", mqToUsers))
                                .build(),
                        message.getSender().getUserId()
                );
            } catch (Exception e) {
                logger.error("send mq failed", e);
            }
        }

        // 按实例分组：实例ID -> 该实例上的用户ID列表
        /*Map<String, Set<String>> instanceToUsers = new HashMap<>();
        for (String userId : targetUserIds) {
            Set<String> instances = userRouteService.getUserInstances(userId);
            if (instances == null || instances.isEmpty()) {
                continue;
            }
            for (String inst : instances) {
                instanceToUsers.computeIfAbsent(inst, k -> new HashSet<>()).add(userId);
            }
        }*/

        // 对本实例的用户直接本地发送，其他实例通过RocketMQ发送
        /*String localInstance = instanceIdProvider.getInstanceId();
        for (Map.Entry<String, Set<String>> entry : instanceToUsers.entrySet()) {
            String targetInstance = entry.getKey();
            Set<String> users = entry.getValue();
            if (targetInstance.equals(localInstance)) {
                // 本地发送
                users.forEach(userId -> userChannelManager.sendToUser(userId, message));
            } else {
                // 发送到RocketMQ
                rocketMQTemplate.syncSendOrderly(
                        iMConfigProperties.getImMsgTopic() + ":" + targetInstance,
                        MessageBuilder
                                .withPayload(message.toByteArray())
                                .setHeader(RocketMQHeader.MSG_ID, message.getMessageId())
                                .setHeader(RocketMQHeader.TARGET_USER_ID, String.join(",", users))
                                .build(),
                        message.getSender().getUserId()
                );
            }
        }*/
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
