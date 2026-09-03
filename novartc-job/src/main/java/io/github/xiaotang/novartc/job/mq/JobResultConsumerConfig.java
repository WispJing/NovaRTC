package io.github.xiaotang.novartc.job.mq;

import io.github.xiaotang.novartc.common.job.JobResultMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import io.github.xiaotang.novartc.job.service.IJobLogService;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "true")
public class JobResultConsumerConfig {
    private final Logger logger = LoggerFactory.getLogger(JobResultConsumerConfig.class);

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Autowired
    private JobMqProperties jobMqProperties;
    @Autowired
    private IJobLogService jobLogService;
    @Autowired
    private IJobInfoService jobInfoService;

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
                    logger.info("收到任务执行结果: logId={}, jobId={}, targetService={}, success={}",
                            result.getLogId(), result.getJobId(), result.getTargetService(), result.getSuccess());
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
    private void updateJobLog(JobResultMessage result) {
        JobLog jobLog = jobLogService.getById(result.getLogId());
        if (jobLog == null) {
            logger.warn("收到任务结果回传，但调度日志不存在: logId={}, jobId={}", result.getLogId(), result.getJobId());
            return;
        }
        JobInfo jobInfo = jobInfoService.getById(jobLog.getJobId());
        if (jobInfo != null && !StringUtils.equals(jobInfo.getTargetService(), result.getTargetService())) {
            jobLog.setExecuteStatus(JobConstants.EXECUTE_FAIL);
            jobLog.setExecuteErrorMsg("执行结果目标服务不一致: expected=" + jobInfo.getTargetService()
                    + ", actual=" + result.getTargetService());
            jobLog.setFinishTime(Instant.now());
            jobLogService.updateById(jobLog);
            logger.error("任务结果服务标识不一致: logId={}, expected={}, actual={}", result.getLogId(),
                    jobInfo.getTargetService(), result.getTargetService());
            return;
        }
        jobLog.setExecuteStatus(Boolean.TRUE.equals(result.getSuccess())
                ? JobConstants.EXECUTE_SUCCESS : JobConstants.EXECUTE_FAIL);
        jobLog.setExecuteErrorMsg(StringUtils.abbreviate(result.getErrorMsg(), 2000));
        jobLog.setExecuteInstance(result.getInstanceId());
        jobLog.setDurationMs(result.getDurationMs());
        jobLog.setFinishTime(Instant.now());
        jobLogService.updateById(jobLog);
        logger.info("任务结果回传更新完成: logId={}, jobId={}, success={}, durationMs={}",
                result.getLogId(), result.getJobId(), result.getSuccess(), result.getDurationMs());
    }

    @PreDestroy
    public void destroy() {
        if (resultConsumer != null) {
            resultConsumer.shutdown();
            logger.info("RocketMQ 任务结果回传消费者已关闭");
        }
    }
}
