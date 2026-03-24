package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;
import io.github.xiaotang.novartc.server.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ChatHistoryPageVO history(@RequestParam String chatId,
                                     @RequestParam(required = false) Long beforeChatSeq,
                                     @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return chatService.history(chatId, beforeChatSeq, limit);
    }
}
