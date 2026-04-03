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

update im_user set is_admin = 0 where is_deleted = 0;
update im_user set is_admin = 1 where id = '9c4a8f6d2b3e4f1a9d7c5e8b2a6f3c10';

delete from admin_user_menu where is_deleted = 0;
delete from admin_role_menu where is_deleted = 0;
delete from admin_user_role where is_deleted = 0;
delete from admin_role where is_deleted = 0;
delete from admin_menu where is_deleted = 0;
delete from admin_operate_log where is_deleted = 0;
delete from admin_login_log where is_deleted = 0;

insert into admin_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_no, status, visible)
values
    ('a1000000000000000000000000000001', '', 'dashboard', '管理总览', 'CATALOG', '/dashboard', 'layouts', 'HomeFilled', 10, 1, 1),
    ('a1000000000000000000000000000002', 'a1000000000000000000000000000001', 'dashboard:view', '管理总览', 'PAGE', '/dashboard', 'dashboard/index', 'HomeFilled', 11, 1, 1),
    ('a1000000000000000000000000000010', '', 'user', '账号管理', 'CATALOG', '/users', 'layouts', 'UserFilled', 20, 1, 1),
    ('a1000000000000000000000000000011', 'a1000000000000000000000000000010', 'user:manage', '账号管理', 'PAGE', '/users', 'users/index', 'UserFilled', 21, 1, 1),
    ('a1000000000000000000000000000012', 'a1000000000000000000000000000010', 'permission:manage', '页面权限', 'PAGE', '/permissions', 'permissions/index', 'Lock', 22, 1, 1),
    ('a1000000000000000000000000000020', '', 'online', '在线管理', 'CATALOG', '/online', 'layouts', 'Monitor', 30, 1, 1),
    ('a1000000000000000000000000000021', 'a1000000000000000000000000000020', 'online:view', '在线管理', 'PAGE', '/online', 'online/index', 'Monitor', 31, 1, 1),
    ('a1000000000000000000000000000022', 'a1000000000000000000000000000020', 'online:kick', '踢下线', 'BUTTON', '', '', '', 32, 1, 0),
    ('a1000000000000000000000000000030', '', 'chat', '会话管理', 'CATALOG', '/chats', 'layouts', 'ChatDotSquare', 40, 1, 1),
    ('a1000000000000000000000000000031', 'a1000000000000000000000000000030', 'chat:view', '会话台账', 'PAGE', '/chats', 'chats/index', 'ChatDotSquare', 41, 1, 1),
    ('a1000000000000000000000000000032', 'a1000000000000000000000000000030', 'group:view', '群组资产', 'PAGE', '/groups', 'groups/index', 'Tickets', 42, 1, 1),
    ('a1000000000000000000000000000033', 'a1000000000000000000000000000030', 'message:audit', '消息审计', 'PAGE', '/history', 'history/index', 'Document', 43, 1, 1),
    ('a1000000000000000000000000000040', '', 'friend-request', '好友审核', 'CATALOG', '/friend-requests', 'layouts', 'Promotion', 50, 1, 1),
    ('a1000000000000000000000000000041', 'a1000000000000000000000000000040', 'friend-request:audit', '申请审核', 'PAGE', '/friend-requests', 'friends/index', 'Promotion', 51, 1, 1),
    ('a1000000000000000000000000000050', '', 'log', '日志审计', 'CATALOG', '/logs', 'layouts', 'Memo', 60, 1, 1),
    ('a1000000000000000000000000000051', 'a1000000000000000000000000000050', 'log:view', '操作日志', 'PAGE', '/logs', 'logs/index', 'Memo', 61, 1, 1);

insert into admin_role (id, role_code, role_name, data_scope, status)
values
    ('b1000000000000000000000000000001', 'OPS_AUDITOR', '运营审计员', 'SELF', 1);

insert into admin_role_menu (id, role_id, menu_id)
values
    ('b2000000000000000000000000000001', 'b1000000000000000000000000000001', 'a1000000000000000000000000000002'),
    ('b2000000000000000000000000000002', 'b1000000000000000000000000000001', 'a1000000000000000000000000000021'),
    ('b2000000000000000000000000000003', 'b1000000000000000000000000000001', 'a1000000000000000000000000000031'),
    ('b2000000000000000000000000000004', 'b1000000000000000000000000000001', 'a1000000000000000000000000000032'),
    ('b2000000000000000000000000000005', 'b1000000000000000000000000000001', 'a1000000000000000000000000000033'),
    ('b2000000000000000000000000000006', 'b1000000000000000000000000000001', 'a1000000000000000000000000000041'),
    ('b2000000000000000000000000000007', 'b1000000000000000000000000000001', 'a1000000000000000000000000000051');

insert into admin_user_role (id, user_id, role_id)
values
    ('b3000000000000000000000000000001', '1f7b3e9c6a2d4c8e5b9a1d3f7c6e2b11', 'b1000000000000000000000000000001');

insert into admin_user_menu (id, user_id, menu_id)
values
    ('b4000000000000000000000000000001', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', 'a1000000000000000000000000000002'),
    ('b4000000000000000000000000000002', '5d2c8a7f1e4b4d9c8f3a2b6e1c7d3f12', 'a1000000000000000000000000000041');

insert into admin_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_no, status, visible)
values
    ('a1000000000000000000000000000060', '', 'risk', '风控中心', 'CATALOG', '/risk-users', 'layouts', 'Warning', 70, 1, 1),
    ('a1000000000000000000000000000061', 'a1000000000000000000000000000060', 'risk:user:view', '用户风控', 'PAGE', '/risk-users', 'risk-users/index', 'UserFilled', 71, 1, 1),
    ('a1000000000000000000000000000062', 'a1000000000000000000000000000060', 'risk:user:edit', '用户风控编辑', 'BUTTON', '', '', '', 72, 1, 0),
    ('a1000000000000000000000000000063', 'a1000000000000000000000000000060', 'risk:group:view', '群禁言', 'PAGE', '/risk-groups', 'risk-groups/index', 'MuteNotification', 73, 1, 1),
    ('a1000000000000000000000000000064', 'a1000000000000000000000000000060', 'risk:group:edit', '群禁言编辑', 'BUTTON', '', '', '', 74, 1, 0),
    ('a1000000000000000000000000000065', 'a1000000000000000000000000000060', 'risk:blacklist:view', '黑名单管理', 'PAGE', '/risk-blacklists', 'risk-blacklists/index', 'CircleClose', 75, 1, 1),
    ('a1000000000000000000000000000066', 'a1000000000000000000000000000060', 'risk:blacklist:edit', '黑名单编辑', 'BUTTON', '', '', '', 76, 1, 0),
    ('a1000000000000000000000000000067', 'a1000000000000000000000000000060', 'risk:alert:view', '登录告警', 'PAGE', '/risk-alerts', 'risk-alerts/index', 'Bell', 77, 1, 1),
    ('a1000000000000000000000000000068', 'a1000000000000000000000000000060', 'risk:alert:handle', '登录告警处理', 'BUTTON', '', '', '', 78, 1, 0);

insert into admin_role_menu (id, role_id, menu_id)
values
    ('b2000000000000000000000000000010', 'b1000000000000000000000000000001', 'a1000000000000000000000000000061'),
    ('b2000000000000000000000000000011', 'b1000000000000000000000000000001', 'a1000000000000000000000000000063'),
    ('b2000000000000000000000000000012', 'b1000000000000000000000000000001', 'a1000000000000000000000000000065'),
    ('b2000000000000000000000000000013', 'b1000000000000000000000000000001', 'a1000000000000000000000000000067');
