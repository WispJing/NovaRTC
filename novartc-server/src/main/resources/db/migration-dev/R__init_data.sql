delete from im_user where is_deleted = 0;
INSERT INTO im_user (id, user_num, username, password, email, avatar_url)
VALUES
    ('9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', 'U1001', '小棠', 'e10adc3949ba59abbe56e057f20f883e', 'xiaotang@example.com', 'https://img.example.com/u1.png'),
    ('1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', 'U1002', '阿杰', 'e10adc3949ba59abbe56e057f20f883e', 'ajie@example.com', 'https://img.example.com/u2.png'),
    ('5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', 'U1003', '小雨', 'e10adc3949ba59abbe56e057f20f883e', 'xiaoyu@example.com', 'https://img.example.com/u3.png'),
    ('7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13', 'U1004', '老王', 'e10adc3949ba59abbe56e057f20f883e', 'laowang@example.com', 'https://img.example.com/u4.png'),
    ('2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14', 'U1005', '小美', 'e10adc3949ba59abbe56e057f20f883e', 'xiaomei@example.com', 'https://img.example.com/u5.png'),
    ('8b1f3c6d9a4e4c2f7d5a8e1b3c6d2a15', 'U1006', '阿强', 'e10adc3949ba59abbe56e057f20f883e', 'aqiang@example.com', 'https://img.example.com/u6.png');

delete from im_friend where is_deleted = 0;
delete from im_friend_request where is_deleted = 0;

INSERT INTO im_friend_request (id, requester_id, receiver_id, status, request_msg, handled_at, handler_id)
VALUES
    ('4f1a7c9d3b2e4e8c6a1d3b7f9c2e6a16', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', 'ACCEPTED', '你好，我是小棠', NOW(), '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11'),
    ('1d7a3c9e5b2f4e8c6a1d3b7f9c2e6a18', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', 'ACCEPTED', '加个好友方便沟通', NOW(), '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12'),
    ('2a7d1c9e5b3f4e8c6a1d3b7f9c2e6a20', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', '7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13', 'ACCEPTED', '以后多交流', NOW(), '7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13'),
    ('3c7a1e9d5b2f4e8c6a1d3b7f9c2e6a22', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14', 'ACCEPTED', '一起聊产品呀', NOW(), '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14'),
    ('5a2c7e1d9b3f4e8c6a1d3b7f2c9e6a24', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14', 'PENDING', '认识一下', NULL, NULL),
    ('8d2e7a1c3b5f4e8c6a1d3b7f9c2e6a26', '8b1f3c6d9a4e4c2f7d5a8e1b3c6d2a15', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', 'REJECTED', '我是阿强', NOW(), '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10');

INSERT INTO im_friend (id, user_id, friend_id, source_request_id)
VALUES
    ('7c2e9a1d3b5f4e8c6a1d3b7f2c9e6a17', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', '4f1a7c9d3b2e4e8c6a1d3b7f9c2e6a16'),
    ('6b3e7a1d5c2f4e8c6a1d3b7f2c9e6a17', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '4f1a7c9d3b2e4e8c6a1d3b7f9c2e6a16'),
    ('8e2c7a1d3b5f4e8c6a1d3b7f2c9e6a19', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', '1d7a3c9e5b2f4e8c6a1d3b7f9c2e6a18'),
    ('7f2c8a1d3b5e4e8c6a1d3b7f9c2e6a19', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '1d7a3c9e5b2f4e8c6a1d3b7f9c2e6a18'),
    ('9b3e7a1d5c2f4e8c6a1d3b7f2c9e6a21', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', '7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13', '2a7d1c9e5b3f4e8c6a1d3b7f9c2e6a20'),
    ('4a7d1c9e5b3f4e8c6a1d3b7f9c2e6a21', '7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', '2a7d1c9e5b3f4e8c6a1d3b7f9c2e6a20'),
    ('6d1e7a9c3b5f4e8c6a1d3b7f2c9e6a23', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14', '3c7a1e9d5b2f4e8c6a1d3b7f9c2e6a22'),
    ('2e7a1c9d5b3f4e8c6a1d3b7f9c2e6a23', '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', '3c7a1e9d5b2f4e8c6a1d3b7f9c2e6a22');

delete from im_group_chat where is_deleted = 0;
INSERT INTO im_group_chat (id, chat_id, group_num, group_name)
VALUES
    ('c2f1e7a9d4b34c8e9a1f5d3b7c2e6a21', '4d9c2a7e5b1f4c8d9a3e6f2b7c1d8a22', 'G10001', '技术交流群'),
    ('a7d3c1e9f5b24e8c9a6d2b7f1c3e4a23', '9e2b7c1d4f6a3c8e5d9a1b2f7c3e6a24', 'G10002', '产品运营群'),
    ('f1c7e3a9d2b64c8e5a1f9d3b7c2e6a25', '2a7d4c1e9f3b6e8c5d1a2b7c9e3f6a26', 'G10003', '摸鱼吹水群');

delete from im_group_member where is_deleted = 0;
INSERT INTO im_group_member (id, group_id, user_id, joined_at)
VALUES
    ('6e2c9a1d4b7f4c8e5a3d2f7b1c9e6a31', 'c2f1e7a9d4b34c8e9a1f5d3b7c2e6a21', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', NOW()),
    ('3b7e1c9a5d2f4e8c6a1d3b9f7c2e6a32', 'c2f1e7a9d4b34c8e9a1f5d3b7c2e6a21', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', NOW()),
    ('8a1d3c7e5b2f4e9c6d1a7b3f2c9e6a33', 'c2f1e7a9d4b34c8e9a1f5d3b7c2e6a21', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', NOW()),
    ('2f6a9c1d7b3e4e8c5a1d3f7b9c2e6a34', 'c2f1e7a9d4b34c8e9a1f5d3b7c2e6a21', '7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13', NOW());

INSERT INTO im_group_member (id, group_id, user_id, joined_at)
VALUES
    ('1a7c3e9d5b2f4e8c6d1a3b7f9c2e6a41', 'a7d3c1e9f5b24e8c9a6d2b7f1c3e4a23', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', NOW()),
    ('9d2f7c1a3e5b4e8c6a1d3b7f2c9e6a42', 'a7d3c1e9f5b24e8c9a6d2b7f1c3e4a23', '7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13', NOW()),
    ('4c1e7a9d3b5f4e8c6a1d2b7f9c3e6a43', 'a7d3c1e9f5b24e8c9a6d2b7f1c3e4a23', '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14', NOW());

INSERT INTO im_group_member (id, group_id, user_id, joined_at)
VALUES
    ('7a3d1c9e5b2f4e8c6a1d3b7f2c9e6a51', 'f1c7e3a9d2b64c8e5a1f9d3b7c2e6a25', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', NOW()),
    ('2c9e7a1d3b5f4e8c6a1d3b7f9c2e6a52', 'f1c7e3a9d2b64c8e5a1f9d3b7c2e6a25', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', NOW()),
    ('5e1a3c7d9b2f4e8c6a1d3b7f2c9e6a53', 'f1c7e3a9d2b64c8e5a1f9d3b7c2e6a25', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', NOW()),
    ('8c2d7a1e3b5f4e8c6a1d3b7f9c2e6a54', 'f1c7e3a9d2b64c8e5a1f9d3b7c2e6a25', '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14', NOW()),
    ('3f7a1c9d5b2e4e8c6a1d3b7f2c9e6a55', 'f1c7e3a9d2b64c8e5a1f9d3b7c2e6a25', '8b1f3c6d9a4e4c2f7d5a8e1b3c6d2a15', NOW());

delete from im_single_chat where is_deleted = 0;
INSERT INTO im_single_chat (id, chat_id, user1_id, user2_id)
VALUES
    ('6b2c9e1a4d7f4c8e5a3d2b7f1c9e6a61', '1c9e7a3d5b2f4e8c6a1d3b7f2c9e6a62', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11'),
    ('3e7a1c9d5b2f4e8c6a1d3b7f9c2e6a63', '7c2e9a1d3b5f4e8c6a1d3b7f2c9e6a64', '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12'),
    ('9a2c7e1d3b5f4e8c6a1d3b7f2c9e6a65', '2e7a1c9d5b3f4e8c6a1d3b7f9c2e6a66', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', '7e3a1c9d4f6b4e2a8d5c7b1f3a9e6c13'),
    ('5c1e7a9d3b2f4e8c6a1d3b7f9c2e6a67', '8a3d1c9e5b2f4e8c6a1d3b7f2c9e6a68', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', '2a6d9e3c5b1f4a7e8c3d6f9b2a1e4c14');
