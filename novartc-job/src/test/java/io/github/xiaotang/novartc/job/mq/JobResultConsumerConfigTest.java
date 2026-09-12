package io.github.xiaotang.novartc.job.mq;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.github.xiaotang.novartc.common.job.JobResultMessage;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import io.github.xiaotang.novartc.job.service.IJobInfoService;
import io.github.xiaotang.novartc.job.service.IJobLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobResultConsumerConfigTest {

    private IJobLogService jobLogService;
    private JobResultConsumerConfig consumer;

    @BeforeEach
    void setUp() {
        jobLogService = mock(IJobLogService.class);
        IJobInfoService jobInfoService = mock(IJobInfoService.class);
        consumer = new JobResultConsumerConfig(new JobMqProperties(), jobLogService, jobInfoService);

        JobLog log = new JobLog();
        log.setId("log-1");
        log.setJobId("job-1");
        JobInfo job = new JobInfo();
        job.setId("job-1");
        job.setTargetService("novartc-server");
        when(jobLogService.getById("log-1")).thenReturn(log);
        when(jobInfoService.getById("job-1")).thenReturn(job);
        when(jobLogService.update(any(JobLog.class), any(Wrapper.class))).thenReturn(true);
    }

    @Test
    void processingResultMovesPendingLogToProcessing() {
        JobResultMessage result = baseResult();
        result.setExecutionStatus(JobResultMessage.STATUS_PROCESSING);
        result.setInstanceId("novartc-server@node-1");
        ArgumentCaptor<JobLog> updateCaptor = ArgumentCaptor.forClass(JobLog.class);

        consumer.updateJobLog(result);

        verify(jobLogService).update(updateCaptor.capture(), any(Wrapper.class));
        assertEquals(JobConstants.EXECUTE_PROCESSING, updateCaptor.getValue().getExecuteStatus());
        assertEquals("novartc-server@node-1", updateCaptor.getValue().getExecuteInstance());
        assertNull(updateCaptor.getValue().getFinishTime());
    }

    @Test
    void legacyResultWithoutExecutionStatusIsCompleted() {
        JobResultMessage result = baseResult();
        result.setSuccess(true);
        result.setDurationMs(25L);
        ArgumentCaptor<JobLog> updateCaptor = ArgumentCaptor.forClass(JobLog.class);

        consumer.updateJobLog(result);

        verify(jobLogService).update(updateCaptor.capture(), any(Wrapper.class));
        assertEquals(JobConstants.EXECUTE_SUCCESS, updateCaptor.getValue().getExecuteStatus());
        assertEquals(25L, updateCaptor.getValue().getDurationMs());
    }

    private JobResultMessage baseResult() {
        JobResultMessage result = new JobResultMessage();
        result.setLogId("log-1");
        result.setJobId("job-1");
        result.setTargetService("novartc-server");
        return result;
    }
}
