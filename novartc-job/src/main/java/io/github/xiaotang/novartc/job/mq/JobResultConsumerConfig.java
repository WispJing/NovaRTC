package io.github.xiaotang.novartc.job.mq;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.xiaotang.novartc.common.job.JobResultMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import io.github.xiaotang.novartc.job.service.IJobLogService;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * 执行结果回传消费者：订阅结果 topic，按调度日志ID回写执行状态。
 *
 * @author xiaotang
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
public class JobResultConsumerConfig {
    private static final Logger logger = LoggerFactory.getLogger(JobResultConsumerConfig.class);

    @Value("${rocketmq.name-server}")
    private String nameServer;

    private final JobMqProperties jobMqProperties;
    private final IJobLogService jobLogService;
    private final IJobInfoService jobInfoService;

    private DefaultMQPushConsumer resultConsumer;

    @Bean
    public DefaultMQPushConsumer jobResultConsumer() throws Exception {
        resultConsumer = new DefaultMQPushConsumer(jobMqProperties.getResultConsumerGroup());
        resultConsumer.setNamesrvAddr(nameServer);
        resultConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        resultConsumer.setMessageModel(MessageModel.CLUSTERING);
        resultConsumer.subscribe(jobMqProperties.getResultTopic(), "*");
        logger.info("RocketMQ 任务结果回传消费者订阅完成: topic={}, group={}",
                jobMqProperties.getResultTopic(), jobMqProperties.getResultConsumerGroup());

        resultConsumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    JobResultMessage result = JacksonUtil.toObject(
                            new String(msg.getBody(), StandardCharsets.UTF_8), JobResultMessage.class);
                    logger.info("收到任务执行状态: logId={}, jobId={}, targetService={}, "
                                    + "executionStatus={}, success={}",
                            result.getLogId(), result.getJobId(), result.getTargetService(),
                            result.getExecutionStatus(), result.getSuccess());
                    updateJobLog(result);
                } catch (Exception e) {
                    logger.error("RocketMQ 任务结果回传消费失败", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        resultConsumer.start();
        logger.info("RocketMQ 任务结果回传消费者已启动");
        return resultConsumer;
    }

    /**
     * 按 logId 回写调度日志执行结果；日志不存在（如已被删除）时仅记录提示。
     */
    void updateJobLog(JobResultMessage result) {
        JobLog jobLog = jobLogService.getById(result.getLogId());
        if (jobLog == null) {
            logger.warn("收到任务结果回传，但调度日志不存在: logId={}, jobId={}", result.getLogId(), result.getJobId());
            return;
        }
        JobInfo jobInfo = jobInfoService.getById(jobLog.getJobId());
        String expectedService = jobInfo == null ? null
                : StringUtils.defaultIfBlank(jobInfo.getTargetService(), "novartc-server");
        String actualService = StringUtils.defaultIfBlank(result.getTargetService(), "novartc-server");
        if (expectedService != null && !StringUtils.equals(expectedService, actualService)) {
            JobLog update = new JobLog();
            update.setExecuteStatus(JobConstants.EXECUTE_FAIL);
            update.setExecuteErrorMsg("执行结果目标服务不一致: expected=" + expectedService
                    + ", actual=" + actualService);
            update.setFinishTime(Instant.now());
            updateUnfinishedLog(result.getLogId(), update);
            logger.error("任务结果服务标识不一致: logId={}, expected={}, actual={}", result.getLogId(),
                    expectedService, actualService);
            return;
        }

        if (JobResultMessage.STATUS_PROCESSING.equals(result.getExecutionStatus())) {
            JobLog update = new JobLog();
            update.setExecuteStatus(JobConstants.EXECUTE_PROCESSING);
            update.setExecuteInstance(result.getInstanceId());
            boolean updated = jobLogService.update(update, new LambdaUpdateWrapper<JobLog>()
                    .eq(JobLog::getId, result.getLogId())
                    .eq(JobLog::getExecuteStatus, JobConstants.EXECUTE_PENDING));
            if (updated) {
                logger.info("任务已进入执行中状态: logId={}, jobId={}, instanceId={}",
                        result.getLogId(), result.getJobId(), result.getInstanceId());
            } else {
                logger.info("忽略重复或迟到的任务执行中状态: logId={}, jobId={}",
                        result.getLogId(), result.getJobId());
            }
            return;
        }

        if (StringUtils.isNotBlank(result.getExecutionStatus())
                && !JobResultMessage.STATUS_COMPLETED.equals(result.getExecutionStatus())) {
            throw new IllegalArgumentException("不支持的任务执行状态: " + result.getExecutionStatus());
        }

        JobLog update = new JobLog();
        update.setExecuteStatus(Boolean.TRUE.equals(result.getSuccess())
                ? JobConstants.EXECUTE_SUCCESS : JobConstants.EXECUTE_FAIL);
        update.setExecuteErrorMsg(StringUtils.abbreviate(result.getErrorMsg(), 2000));
        update.setExecuteInstance(result.getInstanceId());
        update.setDurationMs(result.getDurationMs());
        update.setFinishTime(Instant.now());
        if (updateUnfinishedLog(result.getLogId(), update)) {
            logger.info("任务结果回传更新完成: logId={}, jobId={}, success={}, durationMs={}",
                    result.getLogId(), result.getJobId(), result.getSuccess(), result.getDurationMs());
        } else {
            logger.warn("忽略重复或迟到的任务最终结果: logId={}, jobId={}, success={}",
                    result.getLogId(), result.getJobId(), result.getSuccess());
        }
    }

    private boolean updateUnfinishedLog(String logId, JobLog update) {
        return jobLogService.update(update, new LambdaUpdateWrapper<JobLog>()
                .eq(JobLog::getId, logId)
                .in(JobLog::getExecuteStatus,
                        JobConstants.EXECUTE_PENDING, JobConstants.EXECUTE_PROCESSING));
    }

    @PreDestroy
    public void destroy() {
        if (resultConsumer != null) {
            resultConsumer.shutdown();
            logger.info("RocketMQ 任务结果回传消费者已关闭");
        }
    }
}
