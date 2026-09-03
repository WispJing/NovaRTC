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

import java.time.Instant;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public void handle(JobInvokeMessage message) {
        long started = System.currentTimeMillis();
        boolean success = false;
        String error = null;
        try {
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
            Object[] args = bindArgs(params, handler);
            handler.method().invoke(handler.bean(), args);
            success = true;
        } catch (Exception e) {
            error = e.getCause() == null ? e.toString() : e.getCause().toString();
            logger.error("任务执行失败: logId={}, jobId={}",
                    message.getLogId(), message.getJobId(), e);
        } finally {
            sendResult(message, success, error, started, System.currentTimeMillis());
        }
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

    private void sendResult(JobInvokeMessage message,
                            boolean success,
                            String error,
                            long started,
                            long ended) {
        try {
            JobResultMessage result = new JobResultMessage();
            result.setLogId(message.getLogId());
            result.setJobId(message.getJobId());
            result.setTargetService(properties.getServiceName());
            result.setSuccess(success);
            result.setErrorMsg(StringUtils.abbreviate(error, 2000));
            result.setStartTime(Instant.ofEpochMilli(started).toString());
            result.setFinishTime(Instant.ofEpochMilli(ended).toString());
            result.setDurationMs(ended - started);
            result.setInstanceId(resolveInstanceId());
            SendResult sendResult = template.syncSend(
                    properties.getResultTopic(), JacksonUtil.toJson(result));
            logger.info("任务执行结果已回传: logId={}, jobId={}, success={}, msgId={}",
                    message.getLogId(), message.getJobId(), success, sendResult.getMsgId());
        } catch (Exception e) {
            logger.error("任务执行结果回传失败: logId={}", message.getLogId(), e);
        }
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
}
