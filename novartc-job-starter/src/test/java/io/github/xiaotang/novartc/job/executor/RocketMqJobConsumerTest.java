package io.github.xiaotang.novartc.job.executor;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RocketMqJobConsumerTest {

    @Test
    void retriesOnlyWhenExecutorConfirmsHandlerWasNotInvoked() {
        JobInvokeExecutor executor = mock(JobInvokeExecutor.class);
        when(executor.handle(any(JobInvokeMessage.class)))
                .thenReturn(JobInvokeExecutor.HandleOutcome.RETRY_WITHOUT_EXECUTION);
        RocketMqJobConsumer consumer = new RocketMqJobConsumer(executor, properties());

        ConsumeConcurrentlyStatus status = consumer.consume(List.of(message()));

        assertEquals(ConsumeConcurrentlyStatus.RECONSUME_LATER, status);
    }

    @Test
    void acknowledgesWhenHandlerRanButFinalReportIsPending() {
        JobInvokeExecutor executor = mock(JobInvokeExecutor.class);
        when(executor.handle(any(JobInvokeMessage.class)))
                .thenReturn(JobInvokeExecutor.HandleOutcome.EXECUTED_RESULT_PENDING);
        RocketMqJobConsumer consumer = new RocketMqJobConsumer(executor, properties());

        ConsumeConcurrentlyStatus status = consumer.consume(List.of(message()));

        assertEquals(ConsumeConcurrentlyStatus.CONSUME_SUCCESS, status);
    }

    private JobExecutorProperties properties() {
        JobExecutorProperties properties = new JobExecutorProperties();
        properties.setServiceName("novartc-server");
        properties.setThreadNamePrefix("job-test-");
        return properties;
    }

    private MessageExt message() {
        JobInvokeMessage message = new JobInvokeMessage();
        message.setLogId("log-1");
        message.setJobId("job-1");
        MessageExt raw = new MessageExt();
        raw.setBody(JacksonUtil.toJson(message).getBytes(StandardCharsets.UTF_8));
        return raw;
    }
}
