package io.github.xiaotang.novartc.job.scheduler;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import io.github.xiaotang.novartc.job.mq.JobNotifyProducerService;
import io.github.xiaotang.novartc.job.service.IJobLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobTriggerServiceTest {

    @Test
    void sendFailureImmediatelyFinishesLogAsFailed() {
        IJobLogService jobLogService = mock(IJobLogService.class);
        JobNotifyProducerService producer = mock(JobNotifyProducerService.class);
        when(jobLogService.save(any(JobLog.class))).thenReturn(true);
        when(jobLogService.updateById(any(JobLog.class))).thenReturn(true);
        when(producer.send(any(JobInvokeMessage.class)))
                .thenThrow(new IllegalStateException("broker unavailable"));
        JobTriggerService service = new JobTriggerService(jobLogService, producer);
        ArgumentCaptor<JobLog> logCaptor = ArgumentCaptor.forClass(JobLog.class);

        service.trigger(job(), JobConstants.TRIGGER_TYPE_MANUAL);

        verify(jobLogService, times(1)).updateById(logCaptor.capture());
        JobLog updated = logCaptor.getValue();
        assertEquals(JobConstants.SEND_FAIL, updated.getSendStatus());
        assertEquals(JobConstants.EXECUTE_FAIL, updated.getExecuteStatus());
        assertNotNull(updated.getFinishTime());
    }

    private JobInfo job() {
        JobInfo job = new JobInfo();
        job.setId("job-1");
        job.setJobName("test");
        job.setTargetService("novartc-server");
        job.setInvokeClass("io.github.xiaotang.novartc.server.task.TestTask");
        job.setInvokeMethod("execute");
        job.setTimeoutSeconds(60);
        job.setTimeoutRerun((byte) 0);
        return job;
    }
}
