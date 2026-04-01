package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

@Data
public class AdminChatHistoryQuery {
    private String chatId;
    private Long beforeChatSeq;
    private Integer limit = 20;
}
