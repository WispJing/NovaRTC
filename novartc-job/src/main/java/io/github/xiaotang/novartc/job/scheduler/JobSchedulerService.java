package io.github.xiaotang.novartc.job.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * 调度器管理服务：维护 job_info 表与 Quartz 内存调度器的一致性。
 * <p>
 * job_info 表为任务唯一事实源，启动时全量注册；增删改启停时同步调度器。
 * 注意：调度平台为内存调度，需单实例部署，多实例会重复触发。
 *
 * @author xiaotang
 */
@Service
@RequiredArgsConstructor
public class JobSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(JobSchedulerService.class);

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private final Scheduler scheduler;
    private final IJobInfoService jobInfoService;

    /**
     * 应用启动完成后，将所有启用状态的任务注册到调度器。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerEnabledJobsOnStartup() {
        List<JobInfo> jobs = jobInfoService.list(
                new LambdaQueryWrapper<JobInfo>().eq(JobInfo::getStatus, JobConstants.STATUS_ENABLED));
        int successCount = 0;
        for (JobInfo job : jobs) {
            try {
                schedule(job);
                successCount++;
            } catch (Exception e) {
                logger.error("启动注册定时任务失败: jobId={}, jobName={}", job.getId(), job.getJobName(), e);
            }
        }
        logger.info("定时任务启动注册完成: 启用任务数={}, 注册成功数={}", jobs.size(), successCount);
    }

    /**
     * 注册或更新任务触发器（先移除旧任务，再重新注册）。
     */
    public void schedule(JobInfo job) throws SchedulerException {
        unschedule(job.getId());
        JobDetail jobDetail = buildJobDetail(job);
        Trigger trigger = buildTrigger(job);
        scheduler.scheduleJob(jobDetail, trigger);
        logger.info("定时任务注册完成: jobId={}, jobName={}, scheduleType={}, concurrentAllowed={}",
                job.getId(), job.getJobName(), job.getScheduleType(), job.getConcurrentAllowed());
    }

    /**
     * 移除任务触发器（幂等）。
     */
    public void unschedule(String jobId) throws SchedulerException {
        scheduler.deleteJob(jobKey(jobId));
    }

    /**
     * 查询任务下次执行时间，未注册时返回 null。
     */
    public Date nextFireTime(String jobId) {
        try {
            Trigger trigger = scheduler.getTrigger(triggerKey(jobId));
            return trigger == null ? null : trigger.getNextFireTime();
        } catch (SchedulerException e) {
            logger.warn("查询下次执行时间失败: jobId={}", jobId, e);
            return null;
        }
    }

    /**
     * 根据是否允许并发选择不同的 Quartz Job 实现：
     * 允许并发用 {@link ConcurrentJobNotifyJob}，否则用带 {@code @DisallowConcurrentExecution} 的 {@link SerialJobNotifyJob}。
     */
    private JobDetail buildJobDetail(JobInfo job) {
        Class<? extends Job> jobClass = JobConstants.CONCURRENT_ALLOWED.equals(job.getConcurrentAllowed())
                ? ConcurrentJobNotifyJob.class
                : SerialJobNotifyJob.class;
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey(job.getId()))
                .usingJobData(JobConstants.JOB_DATA_KEY_JOB_ID, job.getId())
                .build();
    }

    private Trigger buildTrigger(JobInfo job) {
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger().withIdentity(triggerKey(job.getId()));
        if (JobConstants.SCHEDULE_TYPE_FIXED_RATE.equals(job.getScheduleType())) {
            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(job.getFixedRateSeconds())
                    .repeatForever()
                    .withMisfireHandlingInstructionNextWithRemainingCount();
            return builder.startNow().withSchedule(scheduleBuilder).build();
        }
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression())
                .inTimeZone(UTC)
                .withMisfireHandlingInstructionDoNothing();
        return builder.withSchedule(scheduleBuilder).build();
    }

    private JobKey jobKey(String jobId) {
        return JobKey.jobKey("job_" + jobId, JobConstants.QUARTZ_GROUP);
    }

    private TriggerKey triggerKey(String jobId) {
        return TriggerKey.triggerKey("job_" + jobId, JobConstants.QUARTZ_GROUP);
    }
}
