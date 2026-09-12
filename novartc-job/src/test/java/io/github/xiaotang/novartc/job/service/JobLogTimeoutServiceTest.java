package io.github.xiaotang.novartc.job.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobLogTimeoutServiceTest {

    @Test
    void convergesUnfinishedLogsToFailure() {
        IJobLogService jobLogService = mock(IJobLogService.class);
        JobMqProperties properties = new JobMqProperties();
        properties.setResultTimeoutSeconds(120);
        when(jobLogService.update(any(JobLog.class), any(Wrapper.class))).thenReturn(true);
        JobLogTimeoutService service = new JobLogTimeoutService(jobLogService, properties);
        Instant now = Instant.parse("2026-09-12T00:00:00Z");
        ArgumentCaptor<JobLog> updateCaptor = ArgumentCaptor.forClass(JobLog.class);

        service.convergeTimedOutLogs(now);

        verify(jobLogService).update(updateCaptor.capture(), any(Wrapper.class));
        JobLog update = updateCaptor.getValue();
        assertEquals(JobConstants.EXECUTE_FAIL, update.getExecuteStatus());
        assertEquals(now, update.getFinishTime());
        assertTrue(update.getExecuteErrorMsg().contains("120 秒"));
    }
}
