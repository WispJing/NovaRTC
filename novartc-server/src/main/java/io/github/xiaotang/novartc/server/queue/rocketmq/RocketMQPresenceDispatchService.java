package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
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

@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RocketMQPresenceDispatchService implements PresenceDispatchService {
    private final Logger logger = LoggerFactory.getLogger(RocketMQPresenceDispatchService.class);
    private final RocketMQTemplate rocketMQTemplate;
    private final UserChannelManager userChannelManager;
    private final InstanceIdProvider instanceIdProvider;
    private final IMConfigProperties imConfigProperties;

    @Override
    public void dispatch(ChatMessage.MsgBody message) {
        userChannelManager.sendToAllUsers(message);
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(
                    imConfigProperties.getPresenceTopic(),
                    MessageBuilder.withPayload(message.toByteArray())
                            .setHeader(RocketMQHeader.MSG_ID, message.getMessageId())
                            .setHeader(RocketMQHeader.SOURCE_INSTANCE_ID, instanceIdProvider.getInstanceId())
                            .build()
            );
            logger.info("RocketMQ presence event send result: {}", JacksonUtil.toJson(sendResult));
        } catch (Exception e) {
            logger.error("Send presence event to RocketMQ failed", e);
        }
    }
}
