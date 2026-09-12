package io.github.xiaotang.novartc.job.scheduler;

import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import lombok.RequiredArgsConstructor;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;

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
        reconcile();
    }

    /**
     * 周期对账以数据库为唯一事实源，补偿事务提交后的 Quartz 同步失败。
     */
    @Scheduled(initialDelayString = "${job.scheduler-reconcile-initial-delay-ms:60000}",
            fixedDelayString = "${job.scheduler-reconcile-interval-ms:60000}")
    public void reconcilePeriodically() {
        reconcile();
    }

    /**
     * 对账启用、停用、删除及调度配置不一致的任务。
     */
    public void reconcile() {
        List<JobInfo> jobs;
        try {
            jobs = jobInfoService.list();
        } catch (Exception e) {
            logger.error("读取任务定义失败，本轮跳过 Quartz 对账", e);
            return;
        }

        Set<JobKey> configuredKeys = new HashSet<>();
        int synchronizedCount = 0;
        for (JobInfo job : jobs) {
            JobKey key = jobKey(job.getId());
            configuredKeys.add(key);
            try {
                if (JobConstants.STATUS_ENABLED.equals(job.getStatus())) {
                    if (!isScheduleInSync(job)) {
                        schedule(job);
                        synchronizedCount++;
                    }
                } else if (scheduler.checkExists(key)) {
                    unschedule(job.getId());
                    synchronizedCount++;
                }
            } catch (Exception e) {
                logger.error("Quartz 任务对账失败: jobId={}, jobName={}",
                        job.getId(), job.getJobName(), e);
            }
        }

        try {
            for (JobKey actualKey : scheduler.getJobKeys(jobGroupEquals(JobConstants.QUARTZ_GROUP))) {
                if (!configuredKeys.contains(actualKey)) {
                    scheduler.deleteJob(actualKey);
                    synchronizedCount++;
                    logger.info("移除数据库中已不存在的 Quartz 任务: jobKey={}", actualKey);
                }
            }
        } catch (SchedulerException e) {
            logger.error("清理无任务定义的 Quartz 任务失败", e);
        }
        logger.info("Quartz 任务对账完成: 数据库任务数={}, 修正数={}", jobs.size(), synchronizedCount);
    }

    /**
     * 注册或更新任务。更新时保留旧触发器直到新触发器成功替换，避免先删除造成任务丢失。
     */
    public void schedule(JobInfo job) throws SchedulerException {
        if (isScheduleInSync(job)) {
            return;
        }

        JobKey key = jobKey(job.getId());
        TriggerKey currentTriggerKey = triggerKey(job.getId());
        JobDetail jobDetail = buildJobDetail(job);
        Trigger trigger = buildTrigger(job);
        if (!scheduler.checkExists(key)) {
            scheduler.scheduleJob(jobDetail, trigger);
        } else {
            JobDetail previousJobDetail = scheduler.getJobDetail(key);
            scheduler.addJob(jobDetail, true, true);
            try {
                if (scheduler.getTrigger(currentTriggerKey) == null) {
                    scheduler.scheduleJob(trigger);
                } else if (scheduler.rescheduleJob(currentTriggerKey, trigger) == null) {
                    throw new SchedulerException("Quartz 未找到待替换的任务触发器: " + currentTriggerKey);
                }
            } catch (SchedulerException e) {
                restoreJobDetail(previousJobDetail, e);
                throw e;
            }
        }
        logger.info("定时任务注册完成: jobId={}, jobName={}, scheduleType={}, concurrentAllowed={}",
                job.getId(), job.getJobName(), job.getScheduleType(), job.getConcurrentAllowed());
    }

    private void restoreJobDetail(JobDetail previousJobDetail, SchedulerException failure) {
        if (previousJobDetail == null) {
            return;
        }
        try {
            scheduler.addJob(previousJobDetail, true, true);
        } catch (SchedulerException restoreFailure) {
            failure.addSuppressed(restoreFailure);
            logger.error("Quartz 新触发器注册失败后，恢复旧任务定义也失败: jobKey={}",
                    previousJobDetail.getKey(), restoreFailure);
        }
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

    boolean isScheduleInSync(JobInfo job) throws SchedulerException {
        JobDetail jobDetail = scheduler.getJobDetail(jobKey(job.getId()));
        Trigger trigger = scheduler.getTrigger(triggerKey(job.getId()));
        if (jobDetail == null || trigger == null
                || !jobKey(job.getId()).equals(trigger.getJobKey())) {
            return false;
        }

        Class<? extends Job> expectedClass = JobConstants.CONCURRENT_ALLOWED.equals(job.getConcurrentAllowed())
                ? ConcurrentJobNotifyJob.class : SerialJobNotifyJob.class;
        if (!expectedClass.equals(jobDetail.getJobClass())) {
            return false;
        }
        if (JobConstants.SCHEDULE_TYPE_FIXED_RATE.equals(job.getScheduleType())) {
            return trigger instanceof SimpleTrigger simpleTrigger
                    && simpleTrigger.getRepeatInterval() == job.getFixedRateSeconds() * 1000L
                    && simpleTrigger.getRepeatCount() == SimpleTrigger.REPEAT_INDEFINITELY;
        }
        return trigger instanceof CronTrigger cronTrigger
                && cronTrigger.getCronExpression().equals(job.getCronExpression())
                && UTC.equals(cronTrigger.getTimeZone());
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
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey(job.getId()))
                .forJob(jobKey(job.getId()));
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
