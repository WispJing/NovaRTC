package io.github.xiaotang.novartc.job.executor;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.common.job.JobResultMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobInvokeExecutorTest {

    private JobHandlerRegistry registry;
    private RocketMQTemplate template;
    private JobExecutorProperties properties;
    private TestHandler handler;
    private JobInvokeExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        registry = mock(JobHandlerRegistry.class);
        template = mock(RocketMQTemplate.class);
        properties = new JobExecutorProperties();
        properties.setServiceName("novartc-server");
        properties.setInstanceId("novartc-server@test");
        properties.setResultSendAttempts(1);
        properties.setResultSendRetryIntervalMs(0);

        handler = new TestHandler();
        Method method = TestHandler.class.getMethod("execute");
        when(registry.require(anyString(), anyString()))
                .thenReturn(new JobHandlerRegistry.Handler(handler, method));
        executor = new JobInvokeExecutor(registry, template, properties);
    }

    @Test
    void processingReportFailureRequestsSafeRetryWithoutInvokingHandler() {
        when(template.syncSend(anyString(), any(Object.class)))
                .thenThrow(new IllegalStateException("broker unavailable"));

        JobInvokeExecutor.HandleOutcome outcome = executor.handle(validMessage());

        assertEquals(JobInvokeExecutor.HandleOutcome.RETRY_WITHOUT_EXECUTION, outcome);
        assertEquals(0, handler.invocations.get());
        verify(template, times(1)).syncSend(anyString(), any(Object.class));
    }

    @Test
    void completedReportFailureDoesNotRequestHandlerReexecution() {
        SendResult sendResult = mock(SendResult.class);
        when(template.syncSend(anyString(), any(Object.class)))
                .thenReturn(sendResult)
                .thenThrow(new IllegalStateException("broker unavailable"));

        JobInvokeExecutor.HandleOutcome outcome = executor.handle(validMessage());

        assertEquals(JobInvokeExecutor.HandleOutcome.EXECUTED_RESULT_PENDING, outcome);
        assertEquals(1, handler.invocations.get());
        verify(template, times(2)).syncSend(anyString(), any(Object.class));
    }

    @Test
    void reportsProcessingBeforeCompletedResult() {
        when(template.syncSend(anyString(), any(Object.class)))
                .thenReturn(mock(SendResult.class));
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        JobInvokeExecutor.HandleOutcome outcome = executor.handle(validMessage());

        assertEquals(JobInvokeExecutor.HandleOutcome.COMPLETED, outcome);
        assertEquals(1, handler.invocations.get());
        verify(template, times(2)).syncSend(anyString(), payloadCaptor.capture());
        List<Object> payloads = payloadCaptor.getAllValues();
        JobResultMessage processing = JacksonUtil.toObject(payloads.get(0).toString(), JobResultMessage.class);
        JobResultMessage completed = JacksonUtil.toObject(payloads.get(1).toString(), JobResultMessage.class);
        assertEquals(JobResultMessage.STATUS_PROCESSING, processing.getExecutionStatus());
        assertNull(processing.getSuccess());
        assertEquals(JobResultMessage.STATUS_COMPLETED, completed.getExecutionStatus());
        assertEquals(Boolean.TRUE, completed.getSuccess());
    }

    private JobInvokeMessage validMessage() {
        JobInvokeMessage message = new JobInvokeMessage();
        message.setLogId("log-1");
        message.setJobId("job-1");
        message.setTargetService("novartc-server");
        message.setInvokeClass(TestHandler.class.getName());
        message.setInvokeMethod("execute");
        message.setInvokeParams("[]");
        message.setTriggerTime(Instant.now().toString());
        message.setTimeoutSeconds(60);
        message.setTimeoutRerun((byte) 0);
        return message;
    }

    public static class TestHandler {
        private final AtomicInteger invocations = new AtomicInteger();

        public void execute() {
            invocations.incrementAndGet();
        }
    }
}
