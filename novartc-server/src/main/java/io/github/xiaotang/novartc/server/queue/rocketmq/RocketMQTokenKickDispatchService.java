package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.queue.TokenKickDispatchService;
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

import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RocketMQTokenKickDispatchService implements TokenKickDispatchService {
    private final Logger logger = LoggerFactory.getLogger(RocketMQTokenKickDispatchService.class);
    private final RocketMQTemplate rocketMQTemplate;
    private final UserChannelManager userChannelManager;
    private final InstanceIdProvider instanceIdProvider;
    private final IMConfigProperties imConfigProperties;

    @Override
    public void dispatch(TokenKickMessage message) {
        userChannelManager.closeByToken(message.getToken());
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(
                    imConfigProperties.getTokenKickTopic(),
                    MessageBuilder.withPayload(JacksonUtil.toJson(message).getBytes(StandardCharsets.UTF_8))
                            .setHeader(RocketMQHeader.SOURCE_INSTANCE_ID, instanceIdProvider.getInstanceId())
                            .build()
            );
            logger.info("RocketMQ token kick event send result: {}", JacksonUtil.toJson(sendResult));
        } catch (Exception e) {
            logger.error("Send token kick event to RocketMQ failed", e);
        }
    }
}
