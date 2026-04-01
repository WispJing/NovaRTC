package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryRequest;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ChatReadRequest;
import io.github.xiaotang.novartc.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IM 会话控制器，提供会话列表、历史消息和已读回执接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    /**
     * 查询当前登录用户的会话列表。
     */
    @PostMapping("/list")
    public List<ChatInfoVO> chatList() {
        return chatService.chatList();
    }

    /**
     * 查询指定会话的历史消息。
     */
    @PostMapping("/history")
    public ChatHistoryPageVO history(@RequestBody ChatHistoryRequest request) {
        return chatService.history(request.getChatId(), request.getBeforeChatSeq(), request.getLimit());
    }

    /**
     * 将指定会话标记为已读。
     */
    @PostMapping("/read")
    public void markRead(@RequestBody ChatReadRequest request) {
        chatService.markRead(request.getChatId());
    }
}
