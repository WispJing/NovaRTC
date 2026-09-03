ALTER TABLE `job_info`
    ADD COLUMN `target_service` VARCHAR(64) NOT NULL DEFAULT 'novartc-server' COMMENT '目标执行微服务标识' AFTER `job_name`;

UPDATE `job_info` SET `target_service` = 'novartc-server' WHERE `target_service` IS NULL OR `target_service` = '';
