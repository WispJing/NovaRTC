package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.dto.AdminChatHistoryQuery;
import io.github.xiaotang.novartc.server.admin.service.AdminConsoleService;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ContactVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台会话控制器，提供会话、群聊和消息审计查询接口。
 */
@RestController
@RequestMapping("/admin/chats")
@RequiredArgsConstructor
public class AdminChatController {
    private final AdminConsoleService adminConsoleService;

    /**
     * 查询后台可见的会话台账。
     */
    @GetMapping
    @AdminPermission("chat:view")
    public List<ChatInfoVO> listChats() {
        return adminConsoleService.listChats(AdminContextHolder.getUserId());
    }

    /**
     * 查询后台可见的群聊资产列表。
     */
    @GetMapping("/groups")
    @AdminPermission("group:view")
    public List<ChatInfoVO> listGroups() {
        return adminConsoleService.listGroups(AdminContextHolder.getUserId());
    }

    /**
     * 查询指定群聊的成员列表。
     */
    @GetMapping("/groups/{groupId}/members")
    @AdminPermission("group:view")
    public List<ContactVO> listGroupMembers(@PathVariable("groupId") String groupId) {
        return adminConsoleService.listGroupMembers(AdminContextHolder.getUserId(), groupId);
    }

    /**
     * 查询指定会话的历史消息。
     */
    @GetMapping("/history")
    @AdminPermission("message:audit")
    public ChatHistoryPageVO history(AdminChatHistoryQuery query) {
        return adminConsoleService.chatHistory(
                AdminContextHolder.getUserId(),
                query.getChatId(),
                query.getBeforeChatSeq(),
                query.getLimit()
        );
    }
}
