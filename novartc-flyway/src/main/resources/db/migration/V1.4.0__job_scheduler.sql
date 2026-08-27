-- 定时任务定义表
CREATE TABLE IF NOT EXISTS `job_info`
(
    `id`                 CHAR(32)     NOT NULL COMMENT 'ID',
    `job_name`           VARCHAR(128) NOT NULL COMMENT '任务名称',
    `invoke_class`       VARCHAR(255) NOT NULL COMMENT '执行类全限定名（NovaRTC 服务侧 Spring Bean）',
    `invoke_method`      VARCHAR(128) NOT NULL COMMENT '执行方法名',
    `invoke_params`      TEXT COMMENT '执行参数（JSON 数组字符串，按方法形参顺序）',
    `schedule_type`      VARCHAR(16)  NOT NULL DEFAULT 'CRON' COMMENT '调度类型：CRON-CRON表达式 FIXED_RATE-固定频率',
    `cron_expression`    VARCHAR(64) COMMENT 'CRON 表达式（UTC 时区）',
    `fixed_rate_seconds` INT COMMENT '固定频率间隔（秒）',
    `concurrent_allowed` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否允许并发执行 0-否 1-是',
    `status`             TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1-启用 0-停用',
    `remark`             VARCHAR(255) COMMENT '备注',

    `is_deleted`         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='定时任务定义表';

-- 定时任务调度日志表
CREATE TABLE IF NOT EXISTS `job_log`
(
    `id`                CHAR(32)      NOT NULL COMMENT 'ID，同时作为 RocketMQ 消息关联ID',
    `job_id`            CHAR(32)      NOT NULL COMMENT '任务ID',
    `job_name`          VARCHAR(128) COMMENT '任务名称快照',
    `trigger_type`      VARCHAR(16)   NOT NULL COMMENT '触发方式 AUTO-定时 MANUAL-手动',
    `invoke_class`      VARCHAR(255) COMMENT '执行类全限定名快照',
    `invoke_method`     VARCHAR(128) COMMENT '执行方法名快照',
    `invoke_params`     TEXT COMMENT '执行参数快照（JSON 数组字符串）',
    `send_status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '消息发送状态 0-失败 1-成功',
    `send_error_msg`    VARCHAR(1000) COMMENT '发送失败原因',
    `execute_status`    TINYINT       NOT NULL DEFAULT 0 COMMENT '执行状态 0-待回传 1-成功 2-失败',
    `execute_error_msg` VARCHAR(2000) COMMENT '执行失败原因',
    `execute_instance`  VARCHAR(64) COMMENT '执行消费实例ID',
    `duration_ms`       BIGINT COMMENT '执行耗时（毫秒）',
    `trigger_time`      DATETIME      NOT NULL COMMENT '触发时间',
    `finish_time`       DATETIME COMMENT '执行结果回传时间',

    `is_deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_job_id` (`job_id`),
    INDEX `idx_trigger_time` (`trigger_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='定时任务调度日志表';
