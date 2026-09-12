package io.github.xiaotang.novartc.job.executor;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.common.job.JobResultMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Objects;

/**
 * 共享任务执行器。
 * <p>
 * 兼容旧消息的超时字段和缺失服务名回退规则，并始终通过注册表执行，禁止基于消息内容进行任意类反射。
 */
public class JobInvokeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JobInvokeExecutor.class);

    private final JobHandlerRegistry registry;
    private final RocketMQTemplate template;
    private final JobExecutorProperties properties;

    public JobInvokeExecutor(JobHandlerRegistry registry,
                             RocketMQTemplate template,
                             JobExecutorProperties properties) {
        this.registry = registry;
        this.template = template;
        this.properties = properties;
    }

    /**
     * 执行任务并同步回传阶段状态。
     * <p>
     * 处理器执行前的回传失败可以安全重投；处理器一旦被调用，最终结果回传失败也不会
     * 请求重投原始任务，避免非幂等处理器被盲目重复执行。
     */
    public HandleOutcome handle(JobInvokeMessage message) {
        Objects.requireNonNull(message, "任务消息不能为空");
        long started = System.currentTimeMillis();
        String instanceId = resolveInstanceId();
        PreparedInvocation invocation;
        try {
            invocation = prepare(message);
        } catch (Exception e) {
            String error = errorMessage(e);
            logger.error("任务执行前校验失败: logId={}, jobId={}",
                    message.getLogId(), message.getJobId(), e);
            JobResultMessage result = completedResult(
                    message, false, error, started, System.currentTimeMillis(), instanceId);
            return sendResult(result)
                    ? HandleOutcome.COMPLETED
                    : HandleOutcome.RETRY_WITHOUT_EXECUTION;
        }

        JobResultMessage processing = processingResult(message, started, instanceId);
        if (!sendResult(processing)) {
            logger.error("任务执行状态回传失败，处理器未执行并请求重投: logId={}, jobId={}",
                    message.getLogId(), message.getJobId());
            return HandleOutcome.RETRY_WITHOUT_EXECUTION;
        }

        boolean success = false;
        String error = null;
        try {
            invocation.handler().method().invoke(invocation.handler().bean(), invocation.args());
            success = true;
        } catch (Exception e) {
            error = errorMessage(e);
            logger.error("任务执行失败: logId={}, jobId={}",
                    message.getLogId(), message.getJobId(), e);
        }

        JobResultMessage result = completedResult(
                message, success, error, started, System.currentTimeMillis(), instanceId);
        if (sendResult(result)) {
            return HandleOutcome.COMPLETED;
        }
        logger.error("任务已执行但最终结果回传失败，将确认原消息以避免重复执行: logId={}, jobId={}",
                message.getLogId(), message.getJobId());
        return HandleOutcome.EXECUTED_RESULT_PENDING;
    }

    private PreparedInvocation prepare(JobInvokeMessage message) {
        String targetService = StringUtils.defaultIfBlank(
                message.getTargetService(), "novartc-server");
        if (!properties.getServiceName().equals(targetService)) {
            throw new IllegalArgumentException("任务目标服务不匹配: " + targetService);
        }
        if (isExpired(message) && !Byte.valueOf((byte) 1).equals(message.getTimeoutRerun())) {
            throw new IllegalStateException("任务消息已过期且未配置重跑");
        }
        JobHandlerRegistry.Handler handler = registry.require(
                message.getInvokeClass(), message.getInvokeMethod());
        JsonNode params = parseParams(message.getInvokeParams());
        if (params.size() != handler.method().getParameterCount()) {
            throw new IllegalArgumentException("任务参数与已注册处理器不匹配");
        }
        return new PreparedInvocation(handler, bindArgs(params, handler));
    }

    boolean isExpired(JobInvokeMessage message) {
        if (StringUtils.isBlank(message.getTriggerTime())) {
            throw new IllegalArgumentException("任务触发时间不能为空");
        }
        try {
            Instant deadline = Instant.parse(message.getTriggerTime())
                    .plusSeconds(timeout(message));
            return Instant.now().isAfter(deadline);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("任务触发时间格式不合法", e);
        }
    }

    private int timeout(JobInvokeMessage message) {
        int value = message.getTimeoutSeconds() == null ? 60 : message.getTimeoutSeconds();
        if (value < 1 || value > 86400) {
            throw new IllegalArgumentException("任务执行超时时间必须在 1-86400 秒之间");
        }
        return value;
    }

    private JsonNode parseParams(String invokeParams) {
        if (StringUtils.isBlank(invokeParams)) {
            return JacksonUtil.getMapper().createArrayNode();
        }
        JsonNode params = JacksonUtil.toObject(invokeParams, JsonNode.class);
        if (params == null || !params.isArray()) {
            throw new IllegalArgumentException("任务执行参数必须是 JSON 数组");
        }
        return params;
    }

    private Object[] bindArgs(JsonNode params, JobHandlerRegistry.Handler handler) {
        Object[] args = new Object[params.size()];
        Class<?>[] parameterTypes = handler.method().getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            args[i] = JacksonUtil.getMapper().convertValue(params.get(i), parameterTypes[i]);
        }
        return args;
    }

    private JobResultMessage processingResult(JobInvokeMessage message,
                                              long started,
                                              String instanceId) {
        JobResultMessage result = baseResult(message, instanceId);
        result.setExecutionStatus(JobResultMessage.STATUS_PROCESSING);
        result.setStartTime(Instant.ofEpochMilli(started).toString());
        return result;
    }

    private JobResultMessage completedResult(JobInvokeMessage message,
                                             boolean success,
                                             String error,
                                             long started,
                                             long ended,
                                             String instanceId) {
        JobResultMessage result = baseResult(message, instanceId);
        result.setExecutionStatus(JobResultMessage.STATUS_COMPLETED);
        result.setSuccess(success);
        result.setErrorMsg(StringUtils.abbreviate(error, 2000));
        result.setStartTime(Instant.ofEpochMilli(started).toString());
        result.setFinishTime(Instant.ofEpochMilli(ended).toString());
        result.setDurationMs(ended - started);
        return result;
    }

    private JobResultMessage baseResult(JobInvokeMessage message, String instanceId) {
        JobResultMessage result = new JobResultMessage();
        result.setLogId(message.getLogId());
        result.setJobId(message.getJobId());
        result.setTargetService(properties.getServiceName());
        result.setInstanceId(instanceId);
        return result;
    }

    private boolean sendResult(JobResultMessage result) {
        int attempts = Math.max(1, properties.getResultSendAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                SendResult sendResult = template.syncSend(
                        properties.getResultTopic(), JacksonUtil.toJson(result));
                logger.info("任务执行状态已回传: logId={}, jobId={}, executionStatus={}, success={}, msgId={}",
                        result.getLogId(), result.getJobId(), result.getExecutionStatus(),
                        result.getSuccess(), sendResult == null ? null : sendResult.getMsgId());
                return true;
            } catch (Exception e) {
                if (attempt == attempts) {
                    logger.error("任务执行状态回传失败: logId={}, executionStatus={}, attempts={}",
                            result.getLogId(), result.getExecutionStatus(), attempts, e);
                    return false;
                }
                logger.warn("任务执行状态回传失败，准备重试: logId={}, executionStatus={}, attempt={}/{}",
                        result.getLogId(), result.getExecutionStatus(), attempt, attempts, e);
                if (!pauseBeforeRetry()) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean pauseBeforeRetry() {
        long interval = Math.max(0, properties.getResultSendRetryIntervalMs());
        if (interval == 0) {
            return true;
        }
        try {
            Thread.sleep(interval);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("任务执行状态回传重试等待被中断");
            return false;
        }
    }

    private String errorMessage(Exception e) {
        return e.getCause() == null ? e.toString() : e.getCause().toString();
    }

    /**
     * Windows 环境通常没有 HOSTNAME，因此不直接依赖单一环境变量。服务名前缀也让
     * 调度日志能够区分不同微服务的执行来源。
     */
    private String resolveInstanceId() {
        if (StringUtils.isNotBlank(properties.getInstanceId())) {
            return properties.getInstanceId();
        }
        String hostName = StringUtils.defaultIfBlank(
                System.getenv("HOSTNAME"), System.getenv("COMPUTERNAME"));
        if (StringUtils.isBlank(hostName)) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                hostName = "unknown-host";
            }
        }
        return properties.getServiceName() + "@" + hostName;
    }

    private record PreparedInvocation(JobHandlerRegistry.Handler handler, Object[] args) {
    }

    public enum HandleOutcome {
        /** 任务未执行且状态无法回传，可安全请求 RocketMQ 重投。 */
        RETRY_WITHOUT_EXECUTION,
        /** 处理器已执行且最终结果已回传。 */
        COMPLETED,
        /** 处理器已执行但最终结果未回传，等待调度端超时收敛。 */
        EXECUTED_RESULT_PENDING
    }
}
