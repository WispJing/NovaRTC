package io.github.xiaotang.novartc.server.task;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.server.config.JobMqProperties;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JobInvokeExecutor} 单元测试：方法定位、参数转换、白名单校验与结果回传。
 */
class JobInvokeExecutorTest {

    private ApplicationContext applicationContext;
    private RocketMQTemplate rocketMQTemplate;
    private JobInvokeExecutor executor;

    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        rocketMQTemplate = mock(RocketMQTemplate.class);
        InstanceIdProvider instanceIdProvider = mock(InstanceIdProvider.class);
        when(instanceIdProvider.getInstanceId()).thenReturn("unit-test-instance");
        when(applicationContext.getBean(JobInvokeTestHandler.class)).thenReturn(new JobInvokeTestHandler());
        when(rocketMQTemplate.syncSend(anyString(), any(Object.class))).thenReturn(new SendResult());
        executor = new JobInvokeExecutor(applicationContext, rocketMQTemplate, new JobMqProperties(), instanceIdProvider);
    }

    /**
     * 无参方法反射执行成功
     */
    @Test
    void invokeWithoutParams() throws Exception {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "demo", null);
        Object result = executor.invoke(message);
        assertNotNull(result);
        assertTrue(result.toString().contains("成功"));
    }

    /**
     * 带参方法按 JSON 数组完成形参类型转换
     */
    @Test
    void invokeWithParamsConvertsTypes() throws Exception {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(),
                "demoWithParams", "[\"单元测试\", 3]");
        Object result = executor.invoke(message);
        assertTrue(result.toString().contains("name=单元测试"));
        assertTrue(result.toString().contains("times=3"));
    }

    /**
     * 白名单外的执行类被拒绝
     */
    @Test
    void invokeRejectsClassOutsideWhitelist() {
        JobInvokeMessage message = buildMessage("java.lang.Runtime", "getRuntime", null);
        assertThrows(IllegalArgumentException.class, () -> executor.invoke(message));
    }

    /**
     * 方法不存在时抛出 NoSuchMethodException
     */
    @Test
    void invokeThrowsWhenMethodMissing() {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "notExist", null);
        assertThrows(NoSuchMethodException.class, () -> executor.invoke(message));
    }

    /**
     * handle 执行完成后向结果 topic 回传成功结果
     */
    @Test
    void handleSendsResultMessage() {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "demo", null);
        message.setLogId("log-001");

        executor.handle(message);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rocketMQTemplate, times(1)).syncSend(eq("novartc-job-result-topic"), payloadCaptor.capture());
        String payload = payloadCaptor.getValue().toString();
        assertTrue(payload.contains("\"success\":true"));
        assertTrue(payload.contains("\"logId\":\"log-001\""));
        assertTrue(payload.contains("\"instanceId\":\"unit-test-instance\""));
    }

    /**
     * handle 在目标方法不存在时回传失败原因
     */
    @Test
    void handleSendsFailureResult() {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "notExist", null);
        message.setLogId("log-002");

        executor.handle(message);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rocketMQTemplate, times(1)).syncSend(eq("novartc-job-result-topic"), payloadCaptor.capture());
        String payload = payloadCaptor.getValue().toString();
        assertTrue(payload.contains("\"success\":false"));
        assertTrue(payload.contains("errorMsg"));
    }

    /**
     * 过期且未配置重跑时跳过执行并回传失败。
     */
    @Test
    void handleSkipsExpiredMessageWhenRerunDisabled() {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "demo", null);
        message.setLogId("log-expired");
        message.setTriggerTime(Instant.now().minusSeconds(61).toString());
        message.setTimeoutSeconds(60);
        message.setTimeoutRerun((byte) 0);

        executor.handle(message);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rocketMQTemplate).syncSend(eq("novartc-job-result-topic"), payloadCaptor.capture());
        String payload = payloadCaptor.getValue().toString();
        assertTrue(payload.contains("\"success\":false"));
        assertTrue(payload.contains("任务消息已过期且未配置重跑"));
    }

    /**
     * 过期但配置重跑时直接补跑当前消息一次。
     */
    @Test
    void handleRunsExpiredMessageWhenRerunEnabled() {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "demo", null);
        message.setLogId("log-rerun");
        message.setTriggerTime(Instant.now().minusSeconds(61).toString());
        message.setTimeoutSeconds(60);
        message.setTimeoutRerun((byte) 1);

        executor.handle(message);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rocketMQTemplate).syncSend(eq("novartc-job-result-topic"), payloadCaptor.capture());
        assertTrue(payloadCaptor.getValue().toString().contains("\"success\":true"));
    }

    /**
     * 旧消息未携带超时时间时默认使用 60 秒。
     */
    @Test
    void oldMessageUsesDefaultTimeout() {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "demo", null);
        message.setTriggerTime(Instant.now().minusSeconds(30).toString());
        message.setTimeoutSeconds(null);

        assertFalse(executor.isExpired(message));
    }

    /**
     * 非法触发时间不执行目标方法并回传失败。
     */
    @Test
    void handleRejectsInvalidTriggerTime() {
        JobInvokeMessage message = buildMessage(JobInvokeTestHandler.class.getName(), "demo", null);
        message.setLogId("log-invalid-time");
        message.setTriggerTime("invalid");

        executor.handle(message);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rocketMQTemplate).syncSend(eq("novartc-job-result-topic"), payloadCaptor.capture());
        String payload = payloadCaptor.getValue().toString();
        assertTrue(payload.contains("\"success\":false"));
        assertTrue(payload.contains("任务触发时间格式不合法"));
    }

    private JobInvokeMessage buildMessage(String invokeClass, String invokeMethod, String invokeParams) {
        JobInvokeMessage message = new JobInvokeMessage();
        message.setLogId("log-test");
        message.setJobId("job-test");
        message.setJobName("单元测试任务");
        message.setInvokeClass(invokeClass);
        message.setInvokeMethod(invokeMethod);
        message.setInvokeParams(invokeParams);
        message.setTriggerType("MANUAL");
        message.setTriggerTime(Instant.now().toString());
        message.setTimeoutSeconds(60);
        message.setTimeoutRerun((byte) 0);
        return message;
    }
}
