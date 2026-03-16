### 连接配置示例（UTC必须）
```bash
spring.datasource.url=jdbc:mysql://localhost:3307/novartc?serverTimezone=UTC
```

### 消息表和已读状态表不存MYSQL
```mysql
-- 消息表
CREATE TABLE IF NOT EXISTS `im_message`
(
    `id`           VARCHAR(64)  NOT NULL COMMENT 'ID',
    `message_id`   VARCHAR(255) NOT NULL COMMENT '业务消息ID',
    `chat_id`      VARCHAR(64)  NOT NULL COMMENT '聊天ID（可能关联group_chat.chat_id或single_chat.chat_id）',
    `sender_id`    VARCHAR(64)  NOT NULL COMMENT '发送者ID',
    `message_text` TEXT COMMENT '消息内容',
    `server_time`  DATETIME COMMENT '服务器时间',
    `client_time`  DATETIME COMMENT '客户端时间',
    `chat_type`    INT COMMENT '聊天类型：1-单聊 2-群聊',
    `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='消息记录表';

-- 已读状态表
CREATE TABLE IF NOT EXISTS `im_read_status`
(
    `id`             VARCHAR(64)  NOT NULL COMMENT 'ID',
    `read_status_id` VARCHAR(255) NOT NULL COMMENT '业务已读状态ID',
    `message_id`     VARCHAR(64)  NOT NULL COMMENT '消息ID（关联message.id）',
    `user_id`        VARCHAR(64)  NOT NULL COMMENT '用户ID（关联user.id）',
    `read_at`        DATETIME COMMENT '阅读时间',
    `is_deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='消息已读状态表';
```
