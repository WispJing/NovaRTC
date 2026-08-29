package io.github.xiaotang.novartc.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 定时任务新增/编辑请求。
 *
 * @author xiaotang
 */
@Data
public class JobSaveRequest {

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称最长 128 个字符")
    private String jobName;

    /**
     * 执行类全限定名（NovaRTC 服务侧 Spring Bean）
     */
    @NotBlank(message = "执行类不能为空")
    @Size(max = 255, message = "执行类最长 255 个字符")
    private String invokeClass;

    /**
     * 执行方法名
     */
    @NotBlank(message = "执行方法不能为空")
    @Size(max = 128, message = "执行方法最长 128 个字符")
    private String invokeMethod;

    /**
     * 执行参数（JSON 数组字符串，按方法形参顺序）
     */
    private String invokeParams;

    /**
     * 调度类型：CRON / FIXED_RATE
     */
    @NotBlank(message = "调度类型不能为空")
    private String scheduleType;

    /**
     * CRON 表达式（UTC 时区）
     */
    @Size(max = 64, message = "CRON 表达式最长 64 个字符")
    private String cronExpression;

    /**
     * 固定频率间隔（秒）
     */
    private Integer fixedRateSeconds;

    /**
     * 执行超时时间（秒），范围 1-86400
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
    @Size(max = 255, message = "备注最长 255 个字符")
    private String remark;
}
