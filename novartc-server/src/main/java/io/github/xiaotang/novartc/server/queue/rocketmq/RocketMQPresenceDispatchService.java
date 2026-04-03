package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.netty.LocalSessionDispatcher;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.PresenceDispatchService;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.RocketMQHeader;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RocketMQPresenceDispatchService implements PresenceDispatchService {
    private final Logger logger = LoggerFactory.getLogger(RocketMQPresenceDispatchService.class);
    private final RocketMQTemplate rocketMQTemplate;
    private final LocalSessionDispatcher localSessionDispatcher;
    private final InstanceIdProvider instanceIdProvider;
    private final IMConfigProperties imConfigProperties;

    @Override
    public void dispatch(ChatMessage.MsgBody message, Set<String> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }
        localSessionDispatcher.sendToUsers(targetUserIds, message);
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(
                    imConfigProperties.getPresenceTopic(),
                    MessageBuilder.withPayload(message.toByteArray())
                            .setHeader(RocketMQHeader.MSG_ID, message.getMessageId())
                            .setHeader(RocketMQHeader.SOURCE_INSTANCE_ID, instanceIdProvider.getInstanceId())
                            .setHeader(RocketMQHeader.TARGET_USER_ID, String.join(",", targetUserIds))
                            .build()
            );
            logger.info("RocketMQ 在线状态事件投递结果: {}", JacksonUtil.toJson(sendResult));
        } catch (Exception e) {
            logger.error("RocketMQ 在线状态事件投递失败", e);
        }
    }
}
