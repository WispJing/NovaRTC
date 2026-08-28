package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * 消息撤回请求。
 */
@Data
public class ChatRecallRequest {
    private String chatId;
    private String messageId;
}
