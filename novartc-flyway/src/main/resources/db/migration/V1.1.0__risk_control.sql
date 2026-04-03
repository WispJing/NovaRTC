-- 风控用户控制表
CREATE TABLE IF NOT EXISTS `risk_user_control`
(
    `id`               CHAR(32)     NOT NULL COMMENT 'ID',
    `user_id`          CHAR(32)     NOT NULL COMMENT '用户ID',
    `control_type`     VARCHAR(20)  NOT NULL COMMENT '控制类型：LOGIN_BAN/SEND_MUTE',
    `scope`            VARCHAR(20)  NOT NULL DEFAULT 'IM' COMMENT '作用域：IM/ADMIN/ALL',
    `reason`           VARCHAR(255)          DEFAULT NULL COMMENT '原因',
    `expires_at`       DATETIME              DEFAULT NULL COMMENT '失效时间，空表示永久',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-关闭，1-生效',
    `operator_user_id` CHAR(32)              DEFAULT NULL COMMENT '操作人ID',
    `operator_name`    VARCHAR(100)          DEFAULT NULL COMMENT '操作人名称',

    `is_deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_user_type_scope` (`user_id`, `control_type`, `scope`),
    INDEX `idx_status_expires` (`status`, `expires_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='风控用户控制表';

-- 风控群禁言表
CREATE TABLE IF NOT EXISTS `risk_group_mute`
(
    `id`               CHAR(32)     NOT NULL COMMENT 'ID',
    `group_id`         CHAR(32)     NOT NULL COMMENT '群组ID',
    `user_id`          CHAR(32)              DEFAULT NULL COMMENT '用户ID，空表示全群禁言',
    `mute_scope`       VARCHAR(20)  NOT NULL COMMENT '禁言范围：GROUP_ALL/USER',
    `reason`           VARCHAR(255)          DEFAULT NULL COMMENT '原因',
    `expires_at`       DATETIME              DEFAULT NULL COMMENT '失效时间，空表示永久',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-关闭，1-生效',
    `operator_user_id` CHAR(32)              DEFAULT NULL COMMENT '操作人ID',
    `operator_name`    VARCHAR(100)          DEFAULT NULL COMMENT '操作人名称',

    `is_deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_group_scope` (`group_id`, `mute_scope`, `status`),
    INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='风控群禁言表';

-- 风控黑名单表
CREATE TABLE IF NOT EXISTS `risk_blacklist`
(
    `id`               CHAR(32)     NOT NULL COMMENT 'ID',
    `owner_user_id`    CHAR(32)     NOT NULL COMMENT '黑名单所属用户ID',
    `blocked_user_id`  CHAR(32)     NOT NULL COMMENT '被拉黑用户ID',
    `source`           VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL/AUTO',
    `reason`           VARCHAR(255)          DEFAULT NULL COMMENT '原因',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-关闭，1-生效',
    `operator_user_id` CHAR(32)              DEFAULT NULL COMMENT '操作人ID',
    `operator_name`    VARCHAR(100)          DEFAULT NULL COMMENT '操作人名称',

    `is_deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_owner_blocked` (`owner_user_id`, `blocked_user_id`),
    INDEX `idx_owner_status` (`owner_user_id`, `status`),
    INDEX `idx_blocked_status` (`blocked_user_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='风控黑名单表';

-- 风控登录告警表
CREATE TABLE IF NOT EXISTS `risk_login_alert`
(
    `id`               CHAR(32)     NOT NULL COMMENT 'ID',
    `user_id`          CHAR(32)              DEFAULT NULL COMMENT '用户ID',
    `user_num`         VARCHAR(64)           DEFAULT NULL COMMENT '用户编号',
    `username`         VARCHAR(100)          DEFAULT NULL COMMENT '用户名',
    `client_type`      VARCHAR(50)           DEFAULT NULL COMMENT '客户端类型',
    `ip`               VARCHAR(64)           DEFAULT NULL COMMENT 'IP地址',
    `user_agent`       VARCHAR(500)          DEFAULT NULL COMMENT '用户代理',
    `alert_type`       VARCHAR(50)  NOT NULL COMMENT '告警类型：LOGIN_BANNED/PASSWORD_ERROR/MULTI_CLIENT',
    `alert_level`      VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM' COMMENT '告警等级：LOW/MEDIUM/HIGH',
    `detail`           VARCHAR(500)          DEFAULT NULL COMMENT '告警详情',
    `handled_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态：0-未处理，1-已处理',
    `handler_user_id`  CHAR(32)              DEFAULT NULL COMMENT '处理人ID',
    `handler_name`     VARCHAR(100)          DEFAULT NULL COMMENT '处理人名称',
    `handled_at`       DATETIME              DEFAULT NULL COMMENT '处理时间',

    `is_deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_user_type` (`user_id`, `alert_type`),
    INDEX `idx_level_status` (`alert_level`, `handled_status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='风控登录告警表';
