package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;

import java.util.List;

/**
 * Created on 2026/3/20 11:35
 *
 * @author tangj
 * @description
 */
public interface ChatService {
    List<ChatInfoVO> chatList();

    ChatHistoryPageVO history(String chatId, Long beforeChatSeq, Integer limit);

    void markRead(String chatId);
}
