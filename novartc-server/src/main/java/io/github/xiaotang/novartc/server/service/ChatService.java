package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;

import java.util.List;

/**
 * IM 会话服务，负责会话列表、历史消息和已读状态查询。
 */
public interface ChatService {
    /**
     * 查询当前用户的会话列表。
     */
    List<ChatInfoVO> chatList();

    /**
     * 分页查询指定会话的历史消息。
     */
    ChatHistoryPageVO history(String chatId, Long beforeChatSeq, Integer limit);

    /**
     * 将指定会话中的未读消息标记为已读。
     */
    void markRead(String chatId);

    /**
     * 撤回当前用户在指定会话中 2 分钟内发送的消息，删除关联文件并通知对端。
     */
    void recall(String chatId, String messageId);
}
