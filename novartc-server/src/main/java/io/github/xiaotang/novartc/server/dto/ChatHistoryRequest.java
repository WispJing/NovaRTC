package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/24 10:53
 *
 * @author tangj
 * @description
 */
@Data
public class ChatHistoryRequest {
    private String chatId;
    private Long beforeChatSeq;
    private Integer limit = 20;
}
