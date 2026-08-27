package io.github.xiaotang.novartc.job.mq;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 定时任务执行通知消息生产者：调度平台 -> NovaRTC 服务。
 *
 * @author xiaotang
 */
@Service
@RequiredArgsConstructor
public class JobNotifyProducerService {

    private static final long SEND_TIMEOUT_MS = 3000L;

    private final RocketMQTemplate rocketMQTemplate;
    private final JobMqProperties jobMqProperties;

    /**
     * 同步发送任务执行通知消息。
     */
    public SendResult send(JobInvokeMessage message) {
        return rocketMQTemplate.syncSend(
                jobMqProperties.getTopic(),
                MessageBuilder.withPayload(JacksonUtil.toJson(message))
                        .setHeader(JobConstants.MQ_HEADER_LOG_ID, message.getLogId())
                        .build(),
                SEND_TIMEOUT_MS
        );
    }
}
