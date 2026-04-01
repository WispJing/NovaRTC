-- 用户表
CREATE TABLE IF NOT EXISTS `im_user`
(
    `id`         CHAR(32) NOT NULL COMMENT 'ID',
    `user_num`   VARCHAR(255) COMMENT '用户编号',
    `username`   VARCHAR(255) COMMENT '用户名',
    `password`   VARCHAR(255) COMMENT '密码哈希值',
    `email`      VARCHAR(255) COMMENT '电子邮箱',
    `avatar_url` VARCHAR(255) COMMENT '头像URL',
    `is_admin`   TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否管理员：0-否，1-是',
    `is_deleted` TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户信息表';

-- 好友申请表
CREATE TABLE IF NOT EXISTS `im_friend_request`
(
    `id`            CHAR(32) NOT NULL COMMENT 'ID',
    `requester_id`  CHAR(32) NOT NULL COMMENT '申请人用户ID',
    `receiver_id`   CHAR(32) NOT NULL COMMENT '接收人用户ID',
    `status`        VARCHAR(20) NOT NULL COMMENT '申请状态：PENDING/ACCEPTED/REJECTED/CANCELED',
    `request_msg`   VARCHAR(255) COMMENT '申请附言',
    `handled_at`    DATETIME COMMENT '处理时间',
    `handler_id`    CHAR(32) COMMENT '处理人用户ID',

    `is_deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_request_pair` (`requester_id`, `receiver_id`),

    INDEX `idx_requester_id` (`requester_id`),
    INDEX `idx_receiver_id` (`receiver_id`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='好友申请表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS `im_friend`
(
    `id`         CHAR(32) NOT NULL COMMENT 'ID',
    `user_id`    CHAR(32) NOT NULL COMMENT '用户ID',
    `friend_id`  CHAR(32) NOT NULL COMMENT '好友用户ID',
    `source_request_id` CHAR(32) COMMENT '来源申请ID',

    `is_deleted` TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),

    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_friend_id` (`friend_id`),
    INDEX `idx_source_request_id` (`source_request_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='好友关系表';

-- 群聊表
CREATE TABLE IF NOT EXISTS `im_group_chat`
(
    `id`         CHAR(32)     NOT NULL COMMENT 'ID',
    `chat_id`    CHAR(32)     NOT NULL COMMENT '聊天会话ID（UUID32）',
    `group_num`  VARCHAR(20)  NOT NULL COMMENT '群号（对外展示）',
    `group_name` VARCHAR(255) NOT NULL COMMENT '群名称',

    `is_deleted` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_chat_id` (`chat_id`),
    UNIQUE KEY `uk_group_num` (`group_num`),

    INDEX `idx_group_name` (`group_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='群聊信息表';

-- 群成员表
CREATE TABLE IF NOT EXISTS `im_group_member`
(
    `id`         CHAR(32) NOT NULL COMMENT 'ID',
    `group_id`   CHAR(32) NOT NULL COMMENT '群聊ID',
    `user_id`    CHAR(32) NOT NULL COMMENT '用户ID',

    `joined_at`  DATETIME COMMENT '加入时间',

    `is_deleted` TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),

    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_group_id` (`group_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='群成员关系表';

-- 单聊表
CREATE TABLE IF NOT EXISTS `im_single_chat`
(
    `id`         CHAR(32) NOT NULL COMMENT 'ID',
    `chat_id`    CHAR(32) NOT NULL COMMENT '聊天会话ID（UUID32）',

    `user1_id`   CHAR(32) NOT NULL COMMENT '用户1ID',
    `user2_id`   CHAR(32) NOT NULL COMMENT '用户2ID',

    `is_deleted` TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    UNIQUE KEY `uk_chat_id` (`chat_id`),

    -- 防止 A-B 和 B-A 出现两条记录（关键设计）
    UNIQUE KEY `uk_user_pair` (`user1_id`, `user2_id`),

    INDEX `idx_user1` (`user1_id`),
    INDEX `idx_user2` (`user2_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='单聊信息表';

-- 后台菜单表
CREATE TABLE IF NOT EXISTS `admin_menu`
(
    `id`         CHAR(32)     NOT NULL COMMENT 'ID',
    `parent_id`  CHAR(32)     NOT NULL DEFAULT '' COMMENT '父级菜单ID，顶级为空字符串',
    `menu_code`  VARCHAR(100) NOT NULL COMMENT '菜单编码',
    `menu_name`  VARCHAR(100) NOT NULL COMMENT '菜单名称',
    `menu_type`  VARCHAR(20)  NOT NULL COMMENT '菜单类型：CATALOG/PAGE/BUTTON',
    `path`       VARCHAR(255)          DEFAULT NULL COMMENT '路由路径',
    `component`  VARCHAR(255)          DEFAULT NULL COMMENT '前端组件路径',
    `icon`       VARCHAR(100)          DEFAULT NULL COMMENT '图标',
    `sort_no`    INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `visible`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示：0-否，1-是',

    `is_deleted` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`menu_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_menu_type` (`menu_type`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台菜单表';

-- 后台角色表
CREATE TABLE IF NOT EXISTS `admin_role`
(
    `id`          CHAR(32)     NOT NULL COMMENT 'ID',
    `role_code`   VARCHAR(100) NOT NULL COMMENT '角色编码',
    `role_name`   VARCHAR(100) NOT NULL COMMENT '角色名称',
    `data_scope`  VARCHAR(20)  NOT NULL DEFAULT 'SELF' COMMENT '数据范围：ALL/SELF/SELF_AND_ASSIGNED',
    `description` VARCHAR(255)          DEFAULT NULL COMMENT '角色描述',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',

    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台角色表';

-- 后台用户角色关系表
CREATE TABLE IF NOT EXISTS `admin_user_role`
(
    `id`         CHAR(32) NOT NULL COMMENT 'ID',
    `user_id`    CHAR(32) NOT NULL COMMENT '用户ID',
    `role_id`    CHAR(32) NOT NULL COMMENT '角色ID',

    `is_deleted` TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台用户角色关系表';

-- 后台角色菜单关系表
CREATE TABLE IF NOT EXISTS `admin_role_menu`
(
    `id`         CHAR(32) NOT NULL COMMENT 'ID',
    `role_id`    CHAR(32) NOT NULL COMMENT '角色ID',
    `menu_id`    CHAR(32) NOT NULL COMMENT '菜单ID',

    `is_deleted` TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_menu_id` (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台角色菜单关系表';

-- 后台用户菜单直授权关系表
CREATE TABLE IF NOT EXISTS `admin_user_menu`
(
    `id`         CHAR(32) NOT NULL COMMENT 'ID',
    `user_id`    CHAR(32) NOT NULL COMMENT '用户ID',
    `menu_id`    CHAR(32) NOT NULL COMMENT '菜单ID',

    `is_deleted` TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_menu` (`user_id`, `menu_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_menu_id` (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台用户菜单直授权关系表';

-- 后台操作日志表
CREATE TABLE IF NOT EXISTS `admin_operate_log`
(
    `id`                CHAR(32)     NOT NULL COMMENT 'ID',
    `operator_user_id`  CHAR(32)              DEFAULT NULL COMMENT '操作人用户ID',
    `operator_user_num` VARCHAR(64)           DEFAULT NULL COMMENT '操作人用户编号',
    `operator_name`     VARCHAR(100)          DEFAULT NULL COMMENT '操作人名称',
    `module`            VARCHAR(100) NOT NULL COMMENT '模块',
    `action`            VARCHAR(100) NOT NULL COMMENT '动作',
    `target_type`       VARCHAR(100)          DEFAULT NULL COMMENT '目标对象类型',
    `target_id`         VARCHAR(100)          DEFAULT NULL COMMENT '目标对象ID',
    `request_method`    VARCHAR(20)           DEFAULT NULL COMMENT '请求方法',
    `request_uri`       VARCHAR(255)          DEFAULT NULL COMMENT '请求路径',
    `request_params`    TEXT                  DEFAULT NULL COMMENT '请求参数',
    `result_code`       VARCHAR(50)           DEFAULT NULL COMMENT '结果码',
    `result_msg`        VARCHAR(255)          DEFAULT NULL COMMENT '结果消息',
    `success`           TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功：0-否，1-是',
    `ip`                VARCHAR(64)           DEFAULT NULL COMMENT 'IP地址',
    `user_agent`        VARCHAR(500)          DEFAULT NULL COMMENT '用户代理',

    `is_deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_operator_user_id` (`operator_user_id`),
    INDEX `idx_module_action` (`module`, `action`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_success` (`success`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台操作日志表';

-- 后台登录日志表
CREATE TABLE IF NOT EXISTS `admin_login_log`
(
    `id`          CHAR(32)     NOT NULL COMMENT 'ID',
    `user_id`     CHAR(32)              DEFAULT NULL COMMENT '用户ID',
    `user_num`    VARCHAR(64)           DEFAULT NULL COMMENT '用户编号',
    `username`    VARCHAR(100)          DEFAULT NULL COMMENT '用户名',
    `login_type`  VARCHAR(20)  NOT NULL DEFAULT 'ADMIN' COMMENT '登录类型：ADMIN/IM',
    `client_type` VARCHAR(50)           DEFAULT NULL COMMENT '客户端类型',
    `token`       VARCHAR(512)          DEFAULT NULL COMMENT '登录token',
    `login_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    `logout_time` DATETIME              DEFAULT NULL COMMENT '登出时间',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '登录状态：0-失败，1-成功，2-已踢下线，3-已登出',
    `fail_reason` VARCHAR(255)          DEFAULT NULL COMMENT '失败原因',
    `ip`          VARCHAR(64)           DEFAULT NULL COMMENT 'IP地址',
    `user_agent`  VARCHAR(500)          DEFAULT NULL COMMENT '用户代理',

    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_login_type` (`login_type`),
    INDEX `idx_client_type` (`client_type`),
    INDEX `idx_status` (`status`),
    INDEX `idx_login_time` (`login_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='后台登录日志表';
