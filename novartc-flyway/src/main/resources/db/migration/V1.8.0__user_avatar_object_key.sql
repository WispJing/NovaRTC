-- im_user 仅保存 MinIO 对象 key，访问地址由应用按当前环境配置动态生成。
UPDATE `im_user`
SET `avatar_url` = CASE
    WHEN `avatar_url` REGEXP '^https?://[^/]+/novartc/.+'
        THEN SUBSTRING(`avatar_url`, LOCATE('/novartc/', `avatar_url`) + CHAR_LENGTH('/novartc/'))
    WHEN `avatar_url` REGEXP '^https?://'
        THEN NULL
    ELSE TRIM(LEADING '/' FROM `avatar_url`)
END
WHERE `avatar_url` IS NOT NULL
  AND `avatar_url` <> '';

ALTER TABLE `im_user`
    MODIFY COLUMN `avatar_url` VARCHAR(255) COMMENT '头像 MinIO 对象 key';
