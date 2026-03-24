package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

import java.util.List;

/**
 * Created on 2026/3/24 10:24
 *
 * @author tangj
 * @description
 */
@Data
public class ChatHistoryPageVO {
    private String chatId;
    private Long nextBeforeChatSeq;
    private boolean hasMore;
    private List<ChatHistoryVO> records;
}
