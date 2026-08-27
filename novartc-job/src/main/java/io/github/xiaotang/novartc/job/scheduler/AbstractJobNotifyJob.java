package io.github.xiaotang.novartc.job.scheduler;

import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Quartz 任务基类：读取任务定义并通过 RocketMQ 通知 NovaRTC 服务执行。
 * <p>
 * 是否允许并发执行由具体子类决定（见 {@link ConcurrentJobNotifyJob}、{@link SerialJobNotifyJob}）。
 *
 * @author xiaotang
 */
public abstract class AbstractJobNotifyJob implements Job {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private JobTriggerService jobTriggerService;

    @Override
    public void execute(JobExecutionContext context) {
        String jobId = context.getMergedJobDataMap().getString(JobConstants.JOB_DATA_KEY_JOB_ID);
        JobInfo jobInfo = jobInfoService.getById(jobId);
        if (jobInfo == null || !JobConstants.STATUS_ENABLED.equals(jobInfo.getStatus())) {
            logger.warn("定时任务不存在或已停用，跳过触发: jobId={}", jobId);
            return;
        }
        jobTriggerService.trigger(jobInfo, JobConstants.TRIGGER_TYPE_AUTO);
    }
}
