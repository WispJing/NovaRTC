package io.github.xiaotang.novartc.netty;

import lombok.Builder;
import lombok.Data;

/**
 * Created on 2026/3/5 14:44
 *
 * @author tangj
 * @description
 */
@Data
@Builder
public class MessageLog {
    private String event;

    private String messageId;

    private String messageType;

    private String senderId;

    private String senderName;

    private Boolean isGroup;

    private String chatId;

    private String targetId;

    private Long timestamp;

    private String content;
}
