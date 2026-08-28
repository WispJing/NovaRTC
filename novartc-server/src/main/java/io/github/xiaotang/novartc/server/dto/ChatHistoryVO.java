package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

import java.time.Instant;

/**
 * Created on 2026/3/24 10:23
 *
 * @author tangj
 * @description
 */
@Data
public class ChatHistoryVO {
    private String messageId;
    private long chatSeq;
    private String chatId;
    private Boolean isGroup;
    private String groupId;
    private String targetUserId;
    private String senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String messageType;
    private String messageText;
    private Object content;
    private Instant serverTime;
    private Instant clientTime;
    private Boolean read;
    private Boolean recalled;
}
