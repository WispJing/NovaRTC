package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on 2026/3/10 14:55
 *
 * @author tangj
 * @description
 */
@Configuration
@ConditionalOnBean(name = "rocketMQTemplate")
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

    @Bean
    public DefaultMQPushConsumer rocketMqConsumer() throws Exception {
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);

        // 动态订阅：Tag 为当前实例 ID
        String instanceId = instanceIdProvider.getInstanceId();
        consumer.subscribe(imConfigProperties.getImMsgTopic(), instanceId);
        logger.info("RocketMQ consumer subscribed topic: {}, tag: {}", imConfigProperties.getImMsgTopic(), instanceId);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    ChatMessage.MsgBody msgBody = ChatMessage.MsgBody.parseFrom(msg.getBody());
                    String msgId = msg.getProperty(RocketMQHeader.MSG_ID);
                    String userStr = msg.getProperty(RocketMQHeader.TARGET_USER_ID);
                    Set<String> targetUserId = Arrays.stream(userStr.split(",")).collect(Collectors.toSet());
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

    @PreDestroy
    public void destroy() {
        if (consumer != null) {
            consumer.shutdown();
            logger.info("RocketMQ consumer shutdown");
        }
    }
}
