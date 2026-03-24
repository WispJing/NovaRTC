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
 * Created on 2026/3/20 11:31
 *
 * @author tangj
 * @description
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/list")
    public List<ChatInfoVO> chatList() {
        return chatService.chatList();
    }

    @PostMapping("/history")
    public ChatHistoryPageVO history(@RequestBody ChatHistoryRequest request) {
        return chatService.history(request.getChatId(), request.getBeforeChatSeq(), request.getLimit());
    }

    @PostMapping("/read")
    public void markRead(@RequestBody ChatReadRequest request) {
        chatService.markRead(request.getChatId());
    }
}
