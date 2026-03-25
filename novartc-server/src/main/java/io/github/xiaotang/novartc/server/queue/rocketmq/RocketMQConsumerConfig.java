package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
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
    @Value("${rocketmq.consumer.group:im_consumer_group}")
    private String consumerGroup;

    @Autowired
    private IMConfigProperties imConfigProperties;
    @Autowired
    private InstanceIdProvider instanceIdProvider;
    @Autowired
    private UserChannelManager userChannelManager;

    private DefaultMQPushConsumer consumer;
    private DefaultMQPushConsumer presenceConsumer;

    @Bean
    public DefaultMQPushConsumer rocketMqConsumer() throws Exception {
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        // consumer.setMessageModel(MessageModel.CLUSTERING);
        consumer.setMessageModel(MessageModel.BROADCASTING);

        // 动态订阅：Tag 为当前实例 ID
        String instanceId = instanceIdProvider.getInstanceId();
        // consumer.subscribe(imConfigProperties.getImMsgTopic(), instanceId);
        consumer.subscribe(imConfigProperties.getImMsgTopic(), "*");
        // logger.info("RocketMQ consumer subscribed topic: {}, tag: {}", imConfigProperties.getImMsgTopic(), instanceId);
        logger.info("RocketMQ consumer subscribed topic: {}, tag: {}", imConfigProperties.getImMsgTopic(), "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    ChatMessage.MsgBody msgBody = ChatMessage.MsgBody.parseFrom(msg.getBody());
                    String msgId = msg.getProperty(RocketMQHeader.MSG_ID);
                    String userStr = msg.getProperty(RocketMQHeader.TARGET_USER_ID);
                    Set<String> targetUserId = Arrays.stream(userStr.split(",")).collect(Collectors.toSet());
                    targetUserId = targetUserId.stream().filter(userChannelManager::isLocalChannel).collect(Collectors.toSet());
                    for (String userId : targetUserId) {
                        userChannelManager.sendToUser(userId, msgBody);
                    }
                } catch (Exception e) {
                    logger.error("Consume message error", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        logger.info("RocketMQ consumer started");
        return consumer;
    }

    @Bean
    public DefaultMQPushConsumer rocketMqPresenceConsumer() throws Exception {
        presenceConsumer = new DefaultMQPushConsumer(imConfigProperties.getPresenceConsumerGroup());
        presenceConsumer.setNamesrvAddr(nameServer);
        presenceConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        presenceConsumer.setMessageModel(MessageModel.BROADCASTING);
        presenceConsumer.subscribe(imConfigProperties.getPresenceTopic(), "*");
        logger.info("RocketMQ presence consumer subscribed topic: {}", imConfigProperties.getPresenceTopic());

        presenceConsumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    String sourceInstanceId = msg.getProperty(RocketMQHeader.SOURCE_INSTANCE_ID);
                    if (instanceIdProvider.getInstanceId().equals(sourceInstanceId)) {
                        continue;
                    }
                    ChatMessage.MsgBody msgBody = ChatMessage.MsgBody.parseFrom(msg.getBody());
                    userChannelManager.sendToAllUsers(msgBody);
                } catch (Exception e) {
                    logger.error("Consume presence message error", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        presenceConsumer.start();
        logger.info("RocketMQ presence consumer started");
        return presenceConsumer;
    }

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
            logger.info("RocketMQ consumer shutdown");
        }
        if (presenceConsumer != null) {
            presenceConsumer.shutdown();
            logger.info("RocketMQ presence consumer shutdown");
        }
    }
}
