ALTER TABLE `job_info`
    ADD COLUMN `timeout_seconds` INT NOT NULL DEFAULT 60 COMMENT '执行超时时间（秒）' AFTER `fixed_rate_seconds`,
    ADD COLUMN `timeout_rerun` TINYINT NOT NULL DEFAULT 0 COMMENT '超时是否重跑 0-否 1-是' AFTER `timeout_seconds`;
