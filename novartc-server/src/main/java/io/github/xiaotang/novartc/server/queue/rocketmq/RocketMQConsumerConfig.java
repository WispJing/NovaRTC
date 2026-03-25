package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.netty.LocalSessionDispatcher;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.RocketMQHeader;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on 2026/3/10 14:55
 *
 * @author tangj
 * @description
 */
@Configuration
public class RocketMQConsumerConfig {
    private final Logger logger = LoggerFactory.getLogger(RocketMQConsumerConfig.class);

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Autowired
    private IMConfigProperties imConfigProperties;
    @Autowired
    private InstanceIdProvider instanceIdProvider;
    @Autowired
    private LocalSessionDispatcher localSessionDispatcher;

    private DefaultMQPushConsumer chatConsumer;
    private DefaultMQPushConsumer presenceConsumer;
    private DefaultMQPushConsumer tokenKickConsumer;

    @Bean
    public DefaultMQPushConsumer chatMessageConsumer() throws Exception {
        chatConsumer = new DefaultMQPushConsumer(imConfigProperties.getChatConsumerGroup());
        chatConsumer.setNamesrvAddr(nameServer);
        chatConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        // chatConsumer.setMessageModel(MessageModel.CLUSTERING);
        chatConsumer.setMessageModel(MessageModel.BROADCASTING);

        // 动态订阅：Tag 为当前实例 ID
        String instanceId = instanceIdProvider.getInstanceId();
        // chatConsumer.subscribe(imConfigProperties.getChatTopic(), instanceId);
        chatConsumer.subscribe(imConfigProperties.getChatTopic(), "*");
        // logger.info("RocketMQ chat consumer subscribed to topic: {}, tag: {}", imConfigProperties.getChatTopic(), instanceId);
        logger.info("RocketMQ chat consumer subscribed to topic: {}, group: {}", imConfigProperties.getChatTopic(), imConfigProperties.getChatConsumerGroup());

        chatConsumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    ChatMessage.MsgBody msgBody = ChatMessage.MsgBody.parseFrom(msg.getBody());
                    String msgId = msg.getProperty(RocketMQHeader.MSG_ID);
                    String userStr = msg.getProperty(RocketMQHeader.TARGET_USER_ID);
                    Set<String> targetUserId = Arrays.stream(userStr.split(",")).collect(Collectors.toSet());
                    targetUserId = targetUserId.stream().filter(localSessionDispatcher::isLocalChannel).collect(Collectors.toSet());
                    for (String userId : targetUserId) {
                        localSessionDispatcher.sendToUser(userId, msgBody);
                    }
                } catch (Exception e) {
                    logger.error("Consume chat message error", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        chatConsumer.start();
        logger.info("RocketMQ chat consumer started");
        return chatConsumer;
    }

    @Bean
    public DefaultMQPushConsumer presenceBroadcastConsumer() throws Exception {
        presenceConsumer = new DefaultMQPushConsumer(imConfigProperties.getPresenceConsumerGroup());
        presenceConsumer.setNamesrvAddr(nameServer);
        presenceConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        presenceConsumer.setMessageModel(MessageModel.BROADCASTING);
        presenceConsumer.subscribe(imConfigProperties.getPresenceTopic(), "*");
        logger.info("RocketMQ presence consumer subscribed to topic: {}, group: {}", imConfigProperties.getPresenceTopic(), imConfigProperties.getPresenceConsumerGroup());

        presenceConsumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    String sourceInstanceId = msg.getProperty(RocketMQHeader.SOURCE_INSTANCE_ID);
                    if (instanceIdProvider.getInstanceId().equals(sourceInstanceId)) {
                        continue;
                    }
                    ChatMessage.MsgBody msgBody = ChatMessage.MsgBody.parseFrom(msg.getBody());
                    String targetUserStr = msg.getProperty(RocketMQHeader.TARGET_USER_ID);
                    if (targetUserStr == null || targetUserStr.isBlank()) {
                        localSessionDispatcher.sendToAllUsers(msgBody);
                    } else {
                        Set<String> targetUserIds = Arrays.stream(targetUserStr.split(","))
                                .filter(userId -> userId != null && !userId.isBlank())
                                .filter(localSessionDispatcher::isLocalChannel)
                                .collect(Collectors.toSet());
                        localSessionDispatcher.sendToUsers(targetUserIds, msgBody);
                    }
                } catch (Exception e) {
                    logger.error("Consume presence event error", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        presenceConsumer.start();
        logger.info("RocketMQ presence consumer started");
        return presenceConsumer;
    }

    @Bean
    public DefaultMQPushConsumer tokenKickConsumer() throws Exception {
        tokenKickConsumer = new DefaultMQPushConsumer(imConfigProperties.getTokenKickConsumerGroup());
        tokenKickConsumer.setNamesrvAddr(nameServer);
        tokenKickConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        tokenKickConsumer.setMessageModel(MessageModel.BROADCASTING);
        tokenKickConsumer.subscribe(imConfigProperties.getTokenKickTopic(), "*");
        logger.info("RocketMQ token kick consumer subscribed to topic: {}, group: {}", imConfigProperties.getTokenKickTopic(), imConfigProperties.getTokenKickConsumerGroup());

        tokenKickConsumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    String sourceInstanceId = msg.getProperty(RocketMQHeader.SOURCE_INSTANCE_ID);
                    if (instanceIdProvider.getInstanceId().equals(sourceInstanceId)) {
                        continue;
                    }
                    TokenKickMessage kickMessage = JacksonUtil.toObject(new String(msg.getBody(), StandardCharsets.UTF_8), TokenKickMessage.class);
                    logger.info("Received token kick message for userId: {}", kickMessage.getUserId());
                    localSessionDispatcher.closeByToken(kickMessage.getToken());
                } catch (Exception e) {
                    logger.error("Consume token kick event error", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        tokenKickConsumer.start();
        logger.info("RocketMQ token kick consumer started");
        return tokenKickConsumer;
    }

    @PreDestroy
    public void destroy() {
        if (chatConsumer != null) {
            chatConsumer.shutdown();
            logger.info("RocketMQ chat consumer shutdown");
        }
        if (presenceConsumer != null) {
            presenceConsumer.shutdown();
            logger.info("RocketMQ presence consumer shutdown");
        }
        if (tokenKickConsumer != null) {
            tokenKickConsumer.shutdown();
            logger.info("RocketMQ token kick consumer shutdown");
        }
    }
}
