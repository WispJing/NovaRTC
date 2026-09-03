package io.github.xiaotang.novartc.job.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.job.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 定时任务定义表
 * </p>
 *
 * @author xiaotang
 * @since 2026-08-28
 */
@Getter
@Setter
@ToString
@TableName("job_info")
public class JobInfo extends BaseEntity {

    @TableField("target_service")
    private String targetService;

    /**
     * 任务名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 执行类全限定名（NovaRTC 服务侧 Spring Bean）
     */
    @TableField("invoke_class")
    private String invokeClass;

    /**
     * 执行方法名
     */
    @TableField("invoke_method")
    private String invokeMethod;

    /**
     * 执行参数（JSON 数组字符串，按方法形参顺序）
     */
    @TableField("invoke_params")
    private String invokeParams;

    /**
     * 调度类型：CRON-CRON表达式 FIXED_RATE-固定频率
     */
    @TableField("schedule_type")
    private String scheduleType;

    /**
     * CRON 表达式（UTC 时区）
     */
    @TableField("cron_expression")
    private String cronExpression;

    /**
     * 固定频率间隔（秒）
     */
    @TableField("fixed_rate_seconds")
    private Integer fixedRateSeconds;

    /**
     * 执行超时时间（秒）
     */
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    /**
     * 超时是否重跑 0-否 1-是
     */
    @TableField("timeout_rerun")
    private Byte timeoutRerun;

    /**
     * 是否允许并发执行 0-否 1-是
     */
    @TableField("concurrent_allowed")
    private Byte concurrentAllowed;

    /**
     * 状态 1-启用 0-停用
     */
    @TableField("status")
    private Byte status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
