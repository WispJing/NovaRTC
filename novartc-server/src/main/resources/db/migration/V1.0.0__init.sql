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
