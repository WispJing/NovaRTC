-- 文件信息表
CREATE TABLE IF NOT EXISTS `im_file`
(
    `id`           CHAR(32)     NOT NULL COMMENT 'ID',
    `biz_type`     VARCHAR(20)  NOT NULL COMMENT '业务类型：avatar/image/video/audio/file',
    `file_name`    VARCHAR(255) COMMENT '原始文件名',
    `object_key`   VARCHAR(255) NOT NULL COMMENT '对象存储key',
    `url`          VARCHAR(500) NOT NULL COMMENT '文件访问URL',
    `size`         BIGINT       COMMENT '文件大小（字节）',
    `content_type` VARCHAR(100) COMMENT 'MIME类型',
    `extension`    VARCHAR(20)  COMMENT '扩展名',
    `uploader_id`  CHAR(32)     NOT NULL COMMENT '上传人用户ID',

    `is_deleted`   TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除',
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    INDEX `idx_uploader_id` (`uploader_id`),
    INDEX `idx_biz_type` (`biz_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='文件信息表';
