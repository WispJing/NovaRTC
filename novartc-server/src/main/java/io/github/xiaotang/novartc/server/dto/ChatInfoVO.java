package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

import java.time.Instant;

/**
 * Created on 2026/3/20 11:33
 *
 * @author tangj
 * @description
 */
@Data
public class ChatInfoVO {

    private String chatType;
    private String chatId;
    private String title;
    private String targetUserId;
    private String targetUserNum;
    private String groupId;
    private String groupNum;
    private String avatarUrl;
    private String lastMessage;
    private String lastMessageType;
    private Instant lastMessageTime;
    private Integer unreadCount;
}
