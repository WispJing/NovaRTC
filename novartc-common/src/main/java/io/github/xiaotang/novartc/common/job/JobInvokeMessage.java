package io.github.xiaotang.novartc.common.job;

import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务执行通知消息：调度平台 -> NovaRTC 服务。
 * <p>
 * 消费实例根据 {@link #invokeClass}、{@link #invokeMethod}、{@link #invokeParams} 反射执行任务。
 *
 * @author xiaotang
 */
@Data
public class JobInvokeMessage implements Serializable {

    /**
     * 调度日志ID（job_log 主键），用于结果回传关联
     */
    private String logId;

    /**
     * 任务ID
     */
    private String jobId;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 执行类全限定名（消费侧 Spring Bean）
     */
    private String invokeClass;

    /**
     * 执行方法名
     */
    private String invokeMethod;

    /**
     * 执行参数（JSON 数组字符串，按方法形参顺序）
     */
    private String invokeParams;

    /**
     * 触发方式：AUTO-定时 MANUAL-手动
     */
    private String triggerType;

    /**
     * 触发时间（UTC ISO-8601）
     */
    private String triggerTime;
}
