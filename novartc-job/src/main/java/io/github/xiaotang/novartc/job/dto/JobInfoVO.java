package io.github.xiaotang.novartc.job.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 定时任务列表展示对象。
 *
 * @author xiaotang
 */
@Data
public class JobInfoVO {

    private String id;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 执行类全限定名
     */
    private String invokeClass;

    /**
     * 执行方法名
     */
    private String invokeMethod;

    /**
     * 执行参数（JSON 数组字符串）
     */
    private String invokeParams;

    /**
     * 调度类型：CRON / FIXED_RATE
     */
    private String scheduleType;

    /**
     * CRON 表达式
     */
    private String cronExpression;

    /**
     * 固定频率间隔（秒）
     */
    private Integer fixedRateSeconds;

    /**
     * 执行超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 超时是否重跑 0-否 1-是
     */
    private Byte timeoutRerun;

    /**
     * 是否允许并发执行 0-否 1-是
     */
    private Byte concurrentAllowed;

    /**
     * 状态 1-启用 0-停用
     */
    private Byte status;

    /**
     * 备注
     */
    private String remark;

    private Instant createdAt;

    private Instant updatedAt;

    /**
     * 下次执行时间（UTC ISO-8601），停用或未注册时为空
     */
    private String nextFireTime;
}
