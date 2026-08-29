package io.github.xiaotang.novartc.server.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.common.job.JobResultMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.JobMqProperties;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 定时任务反射执行器：根据调度平台通知的执行类、方法、参数，
 * 定位 Spring Bean 的公共方法并调用，执行结果经结果 topic 回传调度平台。
 *
 * @author xiaotang
 */
@Service
@RequiredArgsConstructor
public class JobInvokeExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JobInvokeExecutor.class);

    private final ApplicationContext applicationContext;
    private final RocketMQTemplate rocketMQTemplate;
    private final JobMqProperties jobMqProperties;
    private final InstanceIdProvider instanceIdProvider;

    /**
     * 处理一条任务执行通知：反射执行并回传结果（执行失败同样回传失败原因）。
     */
    public void handle(JobInvokeMessage message) {
        long startMillis = System.currentTimeMillis();
        boolean success = false;
        String errorMsg = null;
        try {
            boolean expired = isExpired(message);
            if (expired && !Byte.valueOf((byte) 1).equals(message.getTimeoutRerun())) {
                throw new IllegalStateException("任务消息已过期且未配置重跑");
            }
            if (expired) {
                logger.warn("定时任务消息已过期，按配置补跑一次: logId={}, jobId={}, triggerTime={}, timeoutSeconds={}",
                        message.getLogId(), message.getJobId(), message.getTriggerTime(),
                        effectiveTimeoutSeconds(message));
            }

            Object result = invoke(message);
            success = true;
            logger.info("定时任务执行成功: logId={}, jobId={}, target={}#{}, 结果={}",
                    message.getLogId(), message.getJobId(), message.getInvokeClass(), message.getInvokeMethod(), result);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            errorMsg = cause.toString();
            logger.error("定时任务执行失败(业务异常): logId={}, jobId={}, target={}#{}",
                    message.getLogId(), message.getJobId(), message.getInvokeClass(), message.getInvokeMethod(), cause);
        } catch (Exception e) {
            errorMsg = e.toString();
            logger.error("定时任务执行失败: logId={}, jobId={}, target={}#{}",
                    message.getLogId(), message.getJobId(), message.getInvokeClass(), message.getInvokeMethod(), e);
        } finally {
            sendResult(message, success, errorMsg, startMillis, System.currentTimeMillis());
        }
    }

    /**
     * 判断消息是否超过任务配置的执行窗口。
     */
    boolean isExpired(JobInvokeMessage message) {
        if (StringUtils.isBlank(message.getTriggerTime())) {
            throw new IllegalArgumentException("任务触发时间不能为空");
        }
        Instant triggerTime;
        try {
            triggerTime = Instant.parse(message.getTriggerTime());
        } catch (Exception e) {
            throw new IllegalArgumentException("任务触发时间格式不合法: " + message.getTriggerTime(), e);
        }
        return Instant.now().isAfter(triggerTime.plusSeconds(effectiveTimeoutSeconds(message)));
    }

    /**
     * 兼容未携带超时配置的旧消息。
     */
    private int effectiveTimeoutSeconds(JobInvokeMessage message) {
        Integer timeoutSeconds = message.getTimeoutSeconds();
        if (timeoutSeconds == null) {
            return 60;
        }
        if (timeoutSeconds < 1 || timeoutSeconds > 86400) {
            throw new IllegalArgumentException("任务执行超时时间必须在 1-86400 秒之间");
        }
        return timeoutSeconds;
    }

    /**
     * 定位并调用目标方法。
     * <p>
     * 执行类必须在白名单包前缀内且为 Spring Bean；按方法名与参数个数筛选候选方法，
     * 参数按 JSON 数组顺序逐个转换为形参类型，取首个全部转换成功的公共方法。
     */
    Object invoke(JobInvokeMessage message) throws Exception {
        String className = message.getInvokeClass();
        if (StringUtils.isBlank(className) || !className.startsWith(jobMqProperties.getAllowedPackagePrefix())) {
            throw new IllegalArgumentException("执行类不在白名单范围内: " + className);
        }
        Class<?> clazz = Class.forName(className);
        Object bean = applicationContext.getBean(clazz);
        List<JsonNode> paramNodes = parseParams(message.getInvokeParams());

        ObjectMapper mapper = JacksonUtil.getMapper();
        List<Method> candidates = Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(message.getInvokeMethod()))
                .filter(method -> method.getParameterCount() == paramNodes.size())
                .toList();
        for (Method candidate : candidates) {
            Class<?>[] paramTypes = candidate.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            boolean matched = true;
            for (int i = 0; i < paramTypes.length; i++) {
                try {
                    args[i] = mapper.convertValue(paramNodes.get(i), paramTypes[i]);
                } catch (IllegalArgumentException e) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return candidate.invoke(bean, args);
            }
        }
        throw new NoSuchMethodException("未找到可调用的方法: " + className + "#" + message.getInvokeMethod()
                + ", 参数个数=" + paramNodes.size());
    }

    private List<JsonNode> parseParams(String invokeParams) {
        if (StringUtils.isBlank(invokeParams)) {
            return Collections.emptyList();
        }
        JsonNode node = JacksonUtil.toObject(invokeParams, JsonNode.class);
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException("执行参数必须是 JSON 数组");
        }
        List<JsonNode> paramNodes = new ArrayList<>(node.size());
        node.forEach(paramNodes::add);
        return paramNodes;
    }

    /**
     * 回传执行结果；回传失败仅记录日志，不影响消费确认。
     */
    private void sendResult(JobInvokeMessage message, boolean success, String errorMsg,
                            long startMillis, long finishMillis) {
        try {
            JobResultMessage result = new JobResultMessage();
            result.setLogId(message.getLogId());
            result.setJobId(message.getJobId());
            result.setSuccess(success);
            result.setErrorMsg(StringUtils.abbreviate(errorMsg, 2000));
            result.setStartTime(Instant.ofEpochMilli(startMillis).toString());
            result.setFinishTime(Instant.ofEpochMilli(finishMillis).toString());
            result.setDurationMs(finishMillis - startMillis);
            result.setInstanceId(instanceIdProvider.getInstanceId());
            SendResult sendResult = rocketMQTemplate.syncSend(jobMqProperties.getResultTopic(), JacksonUtil.toJson(result));
            logger.info("任务执行结果回传完成: logId={}, success={}, msgId={}",
                    message.getLogId(), success, sendResult.getMsgId());
        } catch (Exception e) {
            logger.error("任务执行结果回传失败: logId={}", message.getLogId(), e);
        }
    }
}
