package io.github.xiaotang.novartc.job.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** 将长期未收到最终回传的任务日志收敛为明确失败状态。 */
@Service
@RequiredArgsConstructor
public class JobLogTimeoutService {
    private static final Logger logger = LoggerFactory.getLogger(JobLogTimeoutService.class);

    private final IJobLogService jobLogService;
    private final JobMqProperties jobMqProperties;

    @Scheduled(fixedDelayString = "${job.result-timeout-scan-interval-ms:60000}")
    public void convergeTimedOutLogs() {
        convergeTimedOutLogs(Instant.now());
    }

    void convergeTimedOutLogs(Instant now) {
        long timeoutSeconds = Math.max(1, jobMqProperties.getResultTimeoutSeconds());
        Instant cutoff = now.minusSeconds(timeoutSeconds);

        JobLog update = new JobLog();
        update.setExecuteStatus(JobConstants.EXECUTE_FAIL);
        update.setExecuteErrorMsg("任务执行结果回传超时（超过 " + timeoutSeconds
                + " 秒），请核对执行实例与结果 Topic");
        update.setFinishTime(now);

        boolean updated = jobLogService.update(update, new LambdaUpdateWrapper<JobLog>()
                .in(JobLog::getExecuteStatus,
                        JobConstants.EXECUTE_PENDING, JobConstants.EXECUTE_PROCESSING)
                .le(JobLog::getTriggerTime, cutoff));
        if (updated) {
            logger.warn("已将超时未回传的任务日志收敛为失败: cutoff={}, timeoutSeconds={}",
                    cutoff, timeoutSeconds);
        }
    }
}
