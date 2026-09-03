package io.github.xiaotang.novartc.job.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.job.constant.JobConstants;
import io.github.xiaotang.novartc.job.config.JobMqProperties;
import io.github.xiaotang.novartc.job.dto.JobInfoVO;
import io.github.xiaotang.novartc.job.dto.JobSaveRequest;
import io.github.xiaotang.novartc.job.entity.mysql.JobInfo;
import io.github.xiaotang.novartc.job.entity.mysql.JobLog;
import io.github.xiaotang.novartc.job.scheduler.JobSchedulerService;
import io.github.xiaotang.novartc.job.scheduler.JobTriggerService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

/**
 * 定时任务管理服务：负责任务增删改查、启停、手动触发与调度日志查询，
 * 并保证数据库与 Quartz 调度器状态一致。
 *
 * @author xiaotang
 */
@Service
@RequiredArgsConstructor
public class JobManageService {
    private static final Logger logger = LoggerFactory.getLogger(JobManageService.class);

    private final IJobInfoService jobInfoService;
    private final IJobLogService jobLogService;
    private final JobSchedulerService jobSchedulerService;
    private final JobTriggerService jobTriggerService;
    private final JobMqProperties jobMqProperties;

    /**
     * 分页查询任务列表（含下次执行时间）。
     */
    public IPage<JobInfoVO> pageJobs(String keyword, long pageNum, long pageSize) {
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<JobInfo>()
                .like(StringUtils.isNotBlank(keyword), JobInfo::getJobName, keyword)
                .orderByDesc(JobInfo::getCreatedAt);
        IPage<JobInfo> page = jobInfoService.page(new Page<>(pageNum, pageSize), wrapper);
        return page.convert(this::toVO);
    }

    public java.util.List<String> allowedTargetServices() {
        return jobMqProperties.getAllowedServices();
    }

    /**
     * 查询单个任务。
     */
    public JobInfoVO getJob(String id) {
        return toVO(requireJob(id));
    }

    /**
     * 新增任务。
     */
    public JobInfoVO create(JobSaveRequest request) {
        validate(request);
        JobInfo job = new JobInfo();
        job.setId(UUID.randomUUID().toString().replace("-", ""));
        copyFromRequest(request, job);
        jobInfoService.save(job);
        scheduleIfEnabled(job);
        logger.info("新增定时任务: jobId={}, jobName={}", job.getId(), job.getJobName());
        return toVO(job);
    }

    /**
     * 编辑任务（调度配置变更后重新注册触发器）。
     */
    public JobInfoVO update(String id, JobSaveRequest request) {
        validate(request);
        JobInfo job = requireJob(id);
        copyFromRequest(request, job);
        jobInfoService.updateById(job);
        reschedule(job);
        logger.info("编辑定时任务: jobId={}, jobName={}", job.getId(), job.getJobName());
        return toVO(job);
    }

    /**
     * 启用/停用任务。
     */
    public JobInfoVO toggle(String id) {
        JobInfo job = requireJob(id);
        boolean enabled = JobConstants.STATUS_ENABLED.equals(job.getStatus());
        job.setStatus(enabled ? JobConstants.STATUS_DISABLED : JobConstants.STATUS_ENABLED);
        jobInfoService.updateById(job);
        reschedule(job);
        logger.info("切换定时任务状态: jobId={}, jobName={}, status={}", job.getId(), job.getJobName(), job.getStatus());
        return toVO(job);
    }

    /**
     * 删除任务（逻辑删除，调度日志保留）。
     */
    public void delete(String id) {
        JobInfo job = requireJob(id);
        try {
            jobSchedulerService.unschedule(job.getId());
        } catch (SchedulerException e) {
            throw new IllegalStateException("移除调度器任务失败: " + e.getMessage(), e);
        }
        jobInfoService.removeById(job.getId());
        logger.info("删除定时任务: jobId={}, jobName={}", job.getId(), job.getJobName());
    }

    /**
     * 手动执行一次（不经 Quartz，直接发送执行通知消息）。
     */
    public void runNow(String id) {
        JobInfo job = requireJob(id);
        jobTriggerService.trigger(job, JobConstants.TRIGGER_TYPE_MANUAL);
    }

    /**
     * 分页查询任务调度日志（按触发时间倒序）。
     */
    public IPage<JobLog> pageLogs(String jobId, long pageNum, long pageSize) {
        requireJob(jobId);
        LambdaQueryWrapper<JobLog> wrapper = new LambdaQueryWrapper<JobLog>()
                .eq(JobLog::getJobId, jobId)
                .orderByDesc(JobLog::getTriggerTime);
        return jobLogService.page(new Page<>(pageNum, pageSize), wrapper);
    }

    private JobInfo requireJob(String id) {
        JobInfo job = jobInfoService.getById(id);
        if (job == null) {
            throw new IllegalArgumentException("任务不存在: " + id);
        }
        return job;
    }

    private void scheduleIfEnabled(JobInfo job) {
        if (!JobConstants.STATUS_ENABLED.equals(job.getStatus())) {
            return;
        }
        try {
            jobSchedulerService.schedule(job);
        } catch (SchedulerException e) {
            throw new IllegalStateException("任务注册到调度器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 启用则（重新）注册触发器，停用则移除。
     */
    private void reschedule(JobInfo job) {
        try {
            if (JobConstants.STATUS_ENABLED.equals(job.getStatus())) {
                jobSchedulerService.schedule(job);
            } else {
                jobSchedulerService.unschedule(job.getId());
            }
        } catch (SchedulerException e) {
            throw new IllegalStateException("同步调度器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 业务校验：调度类型与表达式、执行类白名单、参数格式。
     */
    private void validate(JobSaveRequest request) {
        if (!jobMqProperties.getAllowedServices().contains(request.getTargetService())) {
            throw new IllegalArgumentException("目标服务未配置或不允许调度: " + request.getTargetService());
        }
        boolean isCron = JobConstants.SCHEDULE_TYPE_CRON.equals(request.getScheduleType());
        boolean isFixedRate = JobConstants.SCHEDULE_TYPE_FIXED_RATE.equals(request.getScheduleType());
        if (!isCron && !isFixedRate) {
            throw new IllegalArgumentException("调度类型仅支持 CRON 或 FIXED_RATE");
        }
        if (isCron) {
            if (StringUtils.isBlank(request.getCronExpression())) {
                throw new IllegalArgumentException("CRON 表达式不能为空");
            }
            if (!CronExpression.isValidExpression(request.getCronExpression())) {
                throw new IllegalArgumentException("CRON 表达式不合法: " + request.getCronExpression());
            }
        } else {
            if (request.getFixedRateSeconds() == null || request.getFixedRateSeconds() < 1) {
                throw new IllegalArgumentException("固定频率间隔必须不小于 1 秒");
            }
        }
        if (!request.getInvokeClass().startsWith(JobConstants.INVOKE_CLASS_PREFIX)) {
            throw new IllegalArgumentException("执行类必须以 " + JobConstants.INVOKE_CLASS_PREFIX + " 开头");
        }
        if (StringUtils.isNotBlank(request.getInvokeParams())) {
            JsonNode node = JacksonUtil.toObject(request.getInvokeParams(), JsonNode.class);
            if (node == null || !node.isArray()) {
                throw new IllegalArgumentException("执行参数必须是 JSON 数组，例如 [\"参数1\", 2]");
            }
        }
        if (request.getConcurrentAllowed() == null) {
            request.setConcurrentAllowed((byte) 0);
        }
        if (request.getTimeoutSeconds() == null) {
            request.setTimeoutSeconds(JobConstants.DEFAULT_TIMEOUT_SECONDS);
        }
        if (request.getTimeoutSeconds() < 1
                || request.getTimeoutSeconds() > JobConstants.MAX_TIMEOUT_SECONDS) {
            throw new IllegalArgumentException("执行超时时间必须在 1-86400 秒之间");
        }
        if (request.getTimeoutRerun() == null) {
            request.setTimeoutRerun((byte) 0);
        }
        if (request.getTimeoutRerun() != 0 && request.getTimeoutRerun() != 1) {
            throw new IllegalArgumentException("超时是否重跑仅支持 0 或 1");
        }
        if (request.getStatus() == null) {
            request.setStatus(JobConstants.STATUS_ENABLED);
        }
    }

    private void copyFromRequest(JobSaveRequest request, JobInfo job) {
        job.setTargetService(request.getTargetService());
        job.setJobName(request.getJobName());
        job.setInvokeClass(request.getInvokeClass());
        job.setInvokeMethod(request.getInvokeMethod());
        job.setInvokeParams(StringUtils.trimToNull(request.getInvokeParams()));
        job.setScheduleType(request.getScheduleType());
        job.setCronExpression(JobConstants.SCHEDULE_TYPE_CRON.equals(request.getScheduleType())
                ? StringUtils.trim(request.getCronExpression()) : null);
        job.setFixedRateSeconds(JobConstants.SCHEDULE_TYPE_FIXED_RATE.equals(request.getScheduleType())
                ? request.getFixedRateSeconds() : null);
        job.setTimeoutSeconds(request.getTimeoutSeconds());
        job.setTimeoutRerun(request.getTimeoutRerun());
        job.setConcurrentAllowed(request.getConcurrentAllowed());
        job.setStatus(request.getStatus());
        job.setRemark(request.getRemark());
    }

    private JobInfoVO toVO(JobInfo job) {
        JobInfoVO vo = new JobInfoVO();
        vo.setTargetService(job.getTargetService());
        vo.setId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setInvokeClass(job.getInvokeClass());
        vo.setInvokeMethod(job.getInvokeMethod());
        vo.setInvokeParams(job.getInvokeParams());
        vo.setScheduleType(job.getScheduleType());
        vo.setCronExpression(job.getCronExpression());
        vo.setFixedRateSeconds(job.getFixedRateSeconds());
        vo.setTimeoutSeconds(job.getTimeoutSeconds());
        vo.setTimeoutRerun(job.getTimeoutRerun());
        vo.setConcurrentAllowed(job.getConcurrentAllowed());
        vo.setStatus(job.getStatus());
        vo.setRemark(job.getRemark());
        vo.setCreatedAt(job.getCreatedAt());
        vo.setUpdatedAt(job.getUpdatedAt());
        Date nextFireTime = jobSchedulerService.nextFireTime(job.getId());
        vo.setNextFireTime(nextFireTime == null ? null : nextFireTime.toInstant().toString());
        return vo;
    }
}
