package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/20 11:33
 *
 * @author tangj
 * @description
 */
@Data
public class ChatInfoVO {

    /**
     * 会话类型：single / group
     */
    private String chatType;

    /**
     * 会话ID，对应 im_single_chat.chat_id / im_group_chat.chat_id
     */
    private String chatId;

    /**
     * 展示标题
     * 单聊：对方用户名
     * 群聊：群名称
     */
    private String title;

    /**
     * 对方用户ID（单聊用）
     */
    private String targetUserId;

    /**
     * 对方账号/编号（单聊可选）
     */
    private String targetUserNum;

    /**
     * 群ID（主键 id，可选）
     */
    private String groupId;

    /**
     * 群号（群聊用）
     */
    private String groupNum;

    /**
     * 头像，可选
     */
    private String avatarUrl;
}
