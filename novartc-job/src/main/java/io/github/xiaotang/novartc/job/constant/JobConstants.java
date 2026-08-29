package io.github.xiaotang.novartc.job.constant;

/**
 * 定时任务调度平台常量。
 *
 * @author xiaotang
 */
public final class JobConstants {

    private JobConstants() {
    }

    /**
     * 调度类型：CRON 表达式
     */
    public static final String SCHEDULE_TYPE_CRON = "CRON";

    /**
     * 调度类型：固定频率（秒）
     */
    public static final String SCHEDULE_TYPE_FIXED_RATE = "FIXED_RATE";

    /**
     * 触发方式：定时自动触发
     */
    public static final String TRIGGER_TYPE_AUTO = "AUTO";

    /**
     * 触发方式：手动触发
     */
    public static final String TRIGGER_TYPE_MANUAL = "MANUAL";

    /**
     * 任务状态：启用
     */
    public static final Byte STATUS_ENABLED = (byte) 1;

    /**
     * 任务状态：停用
     */
    public static final Byte STATUS_DISABLED = (byte) 0;

    /**
     * 允许并发执行
     */
    public static final Byte CONCURRENT_ALLOWED = (byte) 1;

    /**
     * 默认执行超时时间（秒）
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * 最大执行超时时间（秒）
     */
    public static final int MAX_TIMEOUT_SECONDS = 86400;

    /**
     * 消息发送状态：成功
     */
    public static final Byte SEND_SUCCESS = (byte) 1;

    /**
     * 消息发送状态：失败
     */
    public static final Byte SEND_FAIL = (byte) 0;

    /**
     * 执行状态：待回传
     */
    public static final Byte EXECUTE_PENDING = (byte) 0;

    /**
     * 执行状态：成功
     */
    public static final Byte EXECUTE_SUCCESS = (byte) 1;

    /**
     * 执行状态：失败
     */
    public static final Byte EXECUTE_FAIL = (byte) 2;

    /**
     * Quartz JobDataMap 中的任务ID键
     */
    public static final String JOB_DATA_KEY_JOB_ID = "jobId";

    /**
     * Quartz 任务分组
     */
    public static final String QUARTZ_GROUP = "novartc-job";

    /**
     * 执行类白名单前缀，需与消费端 job.allowed-package-prefix 保持一致
     */
    public static final String INVOKE_CLASS_PREFIX = "io.github.xiaotang.novartc";

    /**
     * RocketMQ 消息头：调度日志ID
     */
    public static final String MQ_HEADER_LOG_ID = "JOB_LOG_ID";
}
