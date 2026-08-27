package io.github.xiaotang.novartc.job.scheduler;

import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * {@link JobSchedulerService} 触发器构建单元测试（Quartz 内存调度器）。
 */
class JobSchedulerServiceTest {

    private Scheduler scheduler;
    private JobSchedulerService jobSchedulerService;

    @BeforeEach
    void setUp() throws Exception {
        StdSchedulerFactory factory = new StdSchedulerFactory();
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", "unit-test-scheduler");
        properties.put("org.quartz.threadPool.threadCount", "1");
        properties.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        factory.initialize(properties);
        scheduler = factory.getScheduler();
        jobSchedulerService = new JobSchedulerService(scheduler, mock(IJobInfoService.class));
    }

    @AfterEach
    void tearDown() throws SchedulerException {
        if (scheduler != null) {
            scheduler.shutdown(false);
        }
    }

    /**
     * CRON 任务注册成功，时区为 UTC，禁止并发时使用串行 Job 类
     */
    @Test
    void scheduleCronJob() throws Exception {
        JobInfo job = buildJob("1", JobConstants.SCHEDULE_TYPE_CRON, "0 0/5 * * * ?", null, (byte) 0);

        jobSchedulerService.schedule(job);

        Trigger trigger = scheduler.getTrigger(triggerKey("1"));
        assertNotNull(trigger);
        assertTrue(trigger instanceof CronTrigger);
        assertEquals(TimeZone.getTimeZone("UTC"), ((CronTrigger) trigger).getTimeZone());
        assertEquals("0 0/5 * * * ?", ((CronTrigger) trigger).getCronExpression());
        assertNotNull(trigger.getNextFireTime());

        JobDetail jobDetail = scheduler.getJobDetail(jobKey("1"));
        assertEquals(SerialJobNotifyJob.class, jobDetail.getJobClass());
        assertEquals("1", jobDetail.getJobDataMap().getString(JobConstants.JOB_DATA_KEY_JOB_ID));
    }

    /**
     * 允许并发时使用并发 Job 类
     */
    @Test
    void scheduleConcurrentJob() throws Exception {
        JobInfo job = buildJob("2", JobConstants.SCHEDULE_TYPE_CRON, "0 0 3 * * ?", null, (byte) 1);

        jobSchedulerService.schedule(job);

        JobDetail jobDetail = scheduler.getJobDetail(jobKey("2"));
        assertEquals(ConcurrentJobNotifyJob.class, jobDetail.getJobClass());
    }

    /**
     * 固定频率任务注册为 SimpleTrigger，间隔与配置一致
     */
    @Test
    void scheduleFixedRateJob() throws Exception {
        JobInfo job = buildJob("3", JobConstants.SCHEDULE_TYPE_FIXED_RATE, null, 30, (byte) 0);

        jobSchedulerService.schedule(job);

        Trigger trigger = scheduler.getTrigger(triggerKey("3"));
        assertTrue(trigger instanceof SimpleTrigger);
        assertEquals(30000L, ((SimpleTrigger) trigger).getRepeatInterval());
        assertEquals(SimpleTrigger.REPEAT_INDEFINITELY, ((SimpleTrigger) trigger).getRepeatCount());
    }

    /**
     * 重复注册时旧触发器被替换，不产生重复
     */
    @Test
    void rescheduleReplacesOldTrigger() throws Exception {
        jobSchedulerService.schedule(buildJob("4", JobConstants.SCHEDULE_TYPE_CRON, "0 0/5 * * * ?", null, (byte) 0));
        jobSchedulerService.schedule(buildJob("4", JobConstants.SCHEDULE_TYPE_CRON, "0 0/10 * * * ?", null, (byte) 0));

        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey("4"));
        assertEquals("0 0/10 * * * ?", trigger.getCronExpression());
        assertEquals(1, scheduler.getJobKeys(groupMatcher()).size());
    }

    /**
     * 移除任务后查询不到触发器，且下次执行时间为空
     */
    @Test
    void unscheduleRemovesTrigger() throws Exception {
        jobSchedulerService.schedule(buildJob("5", JobConstants.SCHEDULE_TYPE_CRON, "0 0/5 * * * ?", null, (byte) 0));
        assertNotNull(jobSchedulerService.nextFireTime("5"));

        jobSchedulerService.unschedule("5");

        assertNull(scheduler.getTrigger(triggerKey("5")));
        assertNull(jobSchedulerService.nextFireTime("5"));
        assertFalse(scheduler.checkExists(jobKey("5")));
    }

    private org.quartz.impl.matchers.GroupMatcher<JobKey> groupMatcher() {
        return org.quartz.impl.matchers.GroupMatcher.groupEquals(JobConstants.QUARTZ_GROUP);
    }

    private JobInfo buildJob(String id, String scheduleType, String cronExpression,
                             Integer fixedRateSeconds, byte concurrentAllowed) {
        JobInfo job = new JobInfo();
        job.setId(id);
        job.setJobName("单元测试任务-" + id);
        job.setInvokeClass("io.github.xiaotang.novartc.server.task.JobInvokeDemoHandler");
        job.setInvokeMethod("demo");
        job.setScheduleType(scheduleType);
        job.setCronExpression(cronExpression);
        job.setFixedRateSeconds(fixedRateSeconds);
        job.setConcurrentAllowed(concurrentAllowed);
        job.setStatus(JobConstants.STATUS_ENABLED);
        return job;
    }

    private JobKey jobKey(String jobId) {
        return JobKey.jobKey("job_" + jobId, JobConstants.QUARTZ_GROUP);
    }

    private TriggerKey triggerKey(String jobId) {
        return TriggerKey.triggerKey("job_" + jobId, JobConstants.QUARTZ_GROUP);
    }
}
