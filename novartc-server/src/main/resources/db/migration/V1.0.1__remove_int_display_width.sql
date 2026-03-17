-- 修改 im_user 表的 is_deleted 列，移除显示宽度
ALTER TABLE `im_user`
    MODIFY COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除';

-- 修改 im_group_chat 表的 is_deleted 列
ALTER TABLE `im_group_chat`
    MODIFY COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除';

-- 修改 im_group_member 表的 is_deleted 列
ALTER TABLE `im_group_member`
    MODIFY COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除';

-- 修改 im_single_chat 表的 is_deleted 列
ALTER TABLE `im_single_chat`
    MODIFY COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志 0-未删除 1-已删除';