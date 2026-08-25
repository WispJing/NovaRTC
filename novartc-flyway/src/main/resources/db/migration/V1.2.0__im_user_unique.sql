-- 用户编号与邮箱唯一索引，保证注册数据唯一性
ALTER TABLE `im_user`
    ADD UNIQUE INDEX `uk_im_user_user_num` (`user_num`),
    ADD UNIQUE INDEX `uk_im_user_email` (`email`);
