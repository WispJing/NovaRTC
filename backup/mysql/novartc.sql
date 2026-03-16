-- 用户表
CREATE TABLE IF NOT EXISTS `im_user`
(
    `id`         VARCHAR(64) NOT NULL COMMENT 'ID',
    `user_num`   VARCHAR(255) COMMENT '用户编号',
    `username`   VARCHAR(255) COMMENT '用户名',
    `password`   VARCHAR(255) COMMENT '密码哈希值',
    `email`      VARCHAR(255) COMMENT '电子邮箱',
    `avatar_url` VARCHAR(255) COMMENT '头像URL',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（原MongoDB字段）',
    `is_deleted` TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用户信息表';

-- 群聊表
CREATE TABLE IF NOT EXISTS `im_group_chat`
(
    `id`         VARCHAR(64)  NOT NULL COMMENT 'ID',
    `chat_id`    VARCHAR(255) NOT NULL COMMENT '业务群聊ID',
    `group_num`  VARCHAR(255) NOT NULL COMMENT '群号',
    `group_name` VARCHAR(255) NOT NULL COMMENT '群名称',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（原MongoDB字段）',
    `is_deleted` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='群聊信息表';

-- 群成员表
CREATE TABLE IF NOT EXISTS `im_group_member`
(
    `id`         VARCHAR(64) NOT NULL COMMENT 'ID',
    `group_id`   VARCHAR(64) NOT NULL COMMENT '群聊ID（关联group_chat.id）',
    `user_id`    VARCHAR(64) NOT NULL COMMENT '用户ID（关联user.id）',
    `joined_at`  DATETIME COMMENT '加入时间',
    `is_deleted` TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='群成员关系表';

-- 单聊表
CREATE TABLE IF NOT EXISTS `im_single_chat`
(
    `id`         VARCHAR(64)  NOT NULL COMMENT 'MongoDB原始ID',
    `chat_id`    VARCHAR(255) NOT NULL COMMENT '业务单聊ID',
    `user1_id`   VARCHAR(64)  NOT NULL COMMENT '用户1ID（关联user.id）',
    `user2_id`   VARCHAR(64)  NOT NULL COMMENT '用户2ID（关联user.id）',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（原MongoDB字段）',
    `is_deleted` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='单聊信息表';
