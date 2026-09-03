package io.github.xiaotang.novartc.job.service;

import io.github.xiaotang.novartc.job.dto.JobInfoVO;
import io.github.xiaotang.novartc.job.dto.JobSaveRequest;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.scheduler.JobSchedulerService;
import io.github.xiaotang.novartc.job.scheduler.JobTriggerService;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JobManageService} 超时配置校验与默认值测试。
 */
class JobManageServiceTest {

    private IJobInfoService jobInfoService;
    private JobManageService jobManageService;

    @BeforeEach
    void setUp() {
        jobInfoService = mock(IJobInfoService.class);
        IJobLogService jobLogService = mock(IJobLogService.class);
        JobSchedulerService jobSchedulerService = mock(JobSchedulerService.class);
        JobTriggerService jobTriggerService = mock(JobTriggerService.class);
        when(jobInfoService.save(any(JobInfo.class))).thenReturn(true);
        jobManageService = new JobManageService(
                jobInfoService, jobLogService, jobSchedulerService, jobTriggerService, new JobMqProperties());
    }

    @Test
    void createUsesDefaultTimeoutConfiguration() {
        JobSaveRequest request = validRequest();

        JobInfoVO result = jobManageService.create(request);

        ArgumentCaptor<JobInfo> captor = ArgumentCaptor.forClass(JobInfo.class);
        verify(jobInfoService).save(captor.capture());
        assertEquals(60, captor.getValue().getTimeoutSeconds());
        assertEquals((byte) 0, captor.getValue().getTimeoutRerun());
        assertEquals(60, result.getTimeoutSeconds());
        assertEquals((byte) 0, result.getTimeoutRerun());
    }

    @Test
    void createAcceptsTimeoutBoundaries() {
        JobSaveRequest minimum = validRequest();
        minimum.setTimeoutSeconds(1);
        minimum.setTimeoutRerun((byte) 1);
        JobInfoVO minimumResult = jobManageService.create(minimum);

        JobSaveRequest maximum = validRequest();
        maximum.setTimeoutSeconds(86400);
        JobInfoVO maximumResult = jobManageService.create(maximum);

        assertEquals(1, minimumResult.getTimeoutSeconds());
        assertEquals((byte) 1, minimumResult.getTimeoutRerun());
        assertEquals(86400, maximumResult.getTimeoutSeconds());
    }

    @Test
    void createRejectsTimeoutOutsideRange() {
        JobSaveRequest belowMinimum = validRequest();
        belowMinimum.setTimeoutSeconds(0);
        JobSaveRequest aboveMaximum = validRequest();
        aboveMaximum.setTimeoutSeconds(86401);

        assertThrows(IllegalArgumentException.class, () -> jobManageService.create(belowMinimum));
        assertThrows(IllegalArgumentException.class, () -> jobManageService.create(aboveMaximum));
    }

    @Test
    void createRejectsInvalidTimeoutRerunValue() {
        JobSaveRequest request = validRequest();
        request.setTimeoutRerun((byte) 2);

        assertThrows(IllegalArgumentException.class, () -> jobManageService.create(request));
    }

    private JobSaveRequest validRequest() {
        JobSaveRequest request = new JobSaveRequest();
        request.setJobName("测试任务");
        request.setTargetService("novartc-server");
        request.setInvokeClass("io.github.xiaotang.novartc.server.task.TestTask");
        request.setInvokeMethod("execute");
        request.setScheduleType("CRON");
        request.setCronExpression("0 * * * * ?");
        return request;
    }
}
