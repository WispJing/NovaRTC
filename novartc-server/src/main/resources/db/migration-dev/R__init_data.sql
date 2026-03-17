-- 用户数据 UUID
-- u1: 7f8e9d2c3a5b4b6f8c7d9e0f1a2b3c4d
-- u2: a1b2c3d4e5f678901234567890abcdef
-- u3: 0123456789abcdef0123456789abcdef

INSERT IGNORE INTO `im_user` (`id`, `user_num`, `username`, `password`, `email`, `avatar_url`, `is_deleted`, `created_at`, `updated_at`)
VALUES
    ('7f8e9d2c3a5b4b6f8c7d9e0f1a2b3c4d', '10001', '张三', '123456', 'zhangsan@example.com', 'https://randomuser.me/api/portraits/men/1.jpg', 0, NOW(), NOW()),
    ('a1b2c3d4e5f678901234567890abcdef', '10002', '李四', '123456', 'lisi@example.com', NULL, 0, NOW(), NOW()),
    ('0123456789abcdef0123456789abcdef', '10003', '王五', '123456', 'wangwu@example.com', 'https://randomuser.me/api/portraits/men/2.jpg', 0, NOW(), NOW());

-- 群聊数据 UUID
-- g1: b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0
-- g2: c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1

INSERT IGNORE INTO `im_group_chat` (`id`, `chat_id`, `group_num`, `group_name`, `is_deleted`, `created_at`, `updated_at`)
VALUES
    ('b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0', 'group001', 'G001', '技术交流群', 0, NOW(), NOW()),
    ('c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1', 'group002', 'G002', '闲聊群', 0, NOW(), NOW());

-- 群成员数据 UUID
-- gm1: d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2
-- gm2: e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3
-- gm3: f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4
-- gm4: a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5
-- gm5: b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6

INSERT IGNORE INTO `im_group_member` (`id`, `group_id`, `user_id`, `joined_at`, `is_deleted`, `created_at`, `updated_at`)
VALUES
    -- 技术交流群成员：张三、李四、王五
    ('d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2', 'b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0', '7f8e9d2c3a5b4b6f8c7d9e0f1a2b3c4d', NOW(), 0, NOW(), NOW()),
    ('e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3', 'b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0', 'a1b2c3d4e5f678901234567890abcdef', NOW(), 0, NOW(), NOW()),
    ('f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4', 'b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0', '0123456789abcdef0123456789abcdef', NOW(), 0, NOW(), NOW()),
    -- 闲聊群成员：张三、王五
    ('a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5', 'c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1', '7f8e9d2c3a5b4b6f8c7d9e0f1a2b3c4d', NOW(), 0, NOW(), NOW()),
    ('b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6', 'c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1', '0123456789abcdef0123456789abcdef', NOW(), 0, NOW(), NOW());

-- 单聊数据 UUID
-- s1: c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7
-- s2: d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8
-- s3: e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9

INSERT IGNORE INTO `im_single_chat` (`id`, `chat_id`, `user1_id`, `user2_id`, `is_deleted`, `created_at`, `updated_at`)
VALUES
    ('c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7', 'single001', '7f8e9d2c3a5b4b6f8c7d9e0f1a2b3c4d', 'a1b2c3d4e5f678901234567890abcdef', 0, NOW(), NOW()), -- 张三和李四
    ('d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8', 'single002', '7f8e9d2c3a5b4b6f8c7d9e0f1a2b3c4d', '0123456789abcdef0123456789abcdef', 0, NOW(), NOW()), -- 张三和王五
    ('e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9', 'single003', 'a1b2c3d4e5f678901234567890abcdef', '0123456789abcdef0123456789abcdef', 0, NOW(), NOW()); -- 李四和王五