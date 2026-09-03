package io.github.xiaotang.novartc.job.scheduler;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import io.github.xiaotang.novartc.job.mq.JobNotifyProducerService;
import io.github.xiaotang.novartc.job.service.IJobLogService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 任务触发服务：自动触发（Quartz）与手动触发（页面执行一次）共用。
 * <p>
 * 流程：写入调度日志 -> 组装执行通知消息 -> RocketMQ 发送 -> 回写发送状态。
 *
 * @author xiaotang
 */
@Service
@RequiredArgsConstructor
public class JobTriggerService {
    private static final Logger logger = LoggerFactory.getLogger(JobTriggerService.class);

    private final IJobLogService jobLogService;
    private final JobNotifyProducerService jobNotifyProducerService;

    /**
     * 触发一次任务执行。
     *
     * @param jobInfo     任务定义
     * @param triggerType 触发方式 {@link JobConstants#TRIGGER_TYPE_AUTO} / {@link JobConstants#TRIGGER_TYPE_MANUAL}
     */
    public void trigger(JobInfo jobInfo, String triggerType) {
        Instant triggerTime = Instant.now();

        JobLog jobLog = new JobLog();
        jobLog.setId(UUID.randomUUID().toString().replace("-", ""));
        jobLog.setJobId(jobInfo.getId());
        jobLog.setJobName(jobInfo.getJobName());
        jobLog.setTriggerType(triggerType);
        jobLog.setInvokeClass(jobInfo.getInvokeClass());
        jobLog.setInvokeMethod(jobInfo.getInvokeMethod());
        jobLog.setInvokeParams(jobInfo.getInvokeParams());
        jobLog.setSendStatus(JobConstants.SEND_FAIL);
        jobLog.setExecuteStatus(JobConstants.EXECUTE_PENDING);
        jobLog.setTriggerTime(triggerTime);
        jobLogService.save(jobLog);

        JobInvokeMessage message = new JobInvokeMessage();
        message.setTargetService(jobInfo.getTargetService());
        message.setLogId(jobLog.getId());
        message.setJobId(jobInfo.getId());
        message.setJobName(jobInfo.getJobName());
        message.setInvokeClass(jobInfo.getInvokeClass());
        message.setInvokeMethod(jobInfo.getInvokeMethod());
        message.setInvokeParams(jobInfo.getInvokeParams());
        message.setTriggerType(triggerType);
        message.setTriggerTime(triggerTime.toString());
        message.setTimeoutSeconds(jobInfo.getTimeoutSeconds());
        message.setTimeoutRerun(jobInfo.getTimeoutRerun());

        try {
            SendResult sendResult = jobNotifyProducerService.send(message);
            jobLog.setSendStatus(JobConstants.SEND_SUCCESS);
            jobLogService.updateById(jobLog);
            logger.info("定时任务触发成功: logId={}, jobId={}, jobName={}, triggerType={}, msgId={}",
                    jobLog.getId(), jobInfo.getId(), jobInfo.getJobName(), triggerType, sendResult.getMsgId());
        } catch (Exception e) {
            jobLog.setSendErrorMsg(StringUtils.abbreviate(e.toString(), 1000));
            jobLogService.updateById(jobLog);
            logger.error("定时任务消息发送失败: logId={}, jobId={}, jobName={}",
                    jobLog.getId(), jobInfo.getId(), jobInfo.getJobName(), e);
        }
    }
}
