package io.github.xiaotang.novartc.job.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.job.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * <p>
 * 定时任务调度日志表
 * </p>
 *
 * @author xiaotang
 * @since 2026-08-27
 */
@Getter
@Setter
@ToString
@TableName("job_log")
public class JobLog extends BaseEntity {

    /**
     * 任务ID
     */
    @TableField("job_id")
    private String jobId;

    /**
     * 任务名称快照
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 触发方式 AUTO-定时 MANUAL-手动
     */
    @TableField("trigger_type")
    private String triggerType;

    /**
     * 执行类全限定名快照
     */
    @TableField("invoke_class")
    private String invokeClass;

    /**
     * 执行方法名快照
     */
    @TableField("invoke_method")
    private String invokeMethod;

    /**
     * 执行参数快照（JSON 数组字符串）
     */
    @TableField("invoke_params")
    private String invokeParams;

    /**
     * 消息发送状态 0-失败 1-成功
     */
    @TableField("send_status")
    private Byte sendStatus;

    /**
     * 发送失败原因
     */
    @TableField("send_error_msg")
    private String sendErrorMsg;

    /**
     * 执行状态 0-待回传 1-成功 2-失败
     */
    @TableField("execute_status")
    private Byte executeStatus;

    /**
     * 执行失败原因
     */
    @TableField("execute_error_msg")
    private String executeErrorMsg;

    /**
     * 执行消费实例ID
     */
    @TableField("execute_instance")
    private String executeInstance;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 触发时间
     */
    @TableField("trigger_time")
    private Instant triggerTime;

    /**
     * 执行结果回传时间
     */
    @TableField("finish_time")
    private Instant finishTime;
}
