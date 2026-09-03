package io.github.xiaotang.novartc.common.job;

import lombok.Data;

import java.io.Serializable;

/**
 * 定时任务执行结果回传消息：NovaRTC 服务 -> 调度平台。
 * <p>
 * 调度平台按 {@link #logId} 更新调度日志的执行状态。
 *
 * @author xiaotang
 */
@Data
public class JobResultMessage implements Serializable {

    /** 实际执行服务，用于校验路由与审计。 */
    private String targetService;

    /**
     * 调度日志ID（job_log 主键）
     */
    private String logId;

    /**
     * 任务ID
     */
    private String jobId;

    /**
     * 执行是否成功
     */
    private Boolean success;

    /**
     * 失败原因（成功时为空）
     */
    private String errorMsg;

    /**
     * 执行开始时间（UTC ISO-8601）
     */
    private String startTime;

    /**
     * 执行结束时间（UTC ISO-8601）
     */
    private String finishTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 执行消费实例ID
     */
    private String instanceId;
}
