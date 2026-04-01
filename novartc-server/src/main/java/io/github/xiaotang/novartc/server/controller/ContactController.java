package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ContactVO;
import io.github.xiaotang.novartc.server.dto.FriendRequestCreateRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestHandleRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.dto.StartChatRequest;
import io.github.xiaotang.novartc.server.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * IM 联系人控制器，提供联系人、单聊发起和好友申请相关接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactService contactService;

    /**
     * 查询当前登录用户的联系人列表。
     */
    @PostMapping("/list")
    public List<ContactVO> list() {
        return contactService.listContacts(UserContextHolder.getUserId());
    }

    /**
     * 发起或获取与指定用户的单聊会话。
     */
    @PostMapping("/start")
    public ChatInfoVO start(@RequestBody StartChatRequest request) {
        return contactService.startChat(UserContextHolder.getUserId(), request);
    }

    /**
     * 查询当前登录用户相关的好友申请列表。
     */
    @PostMapping("/request/list")
    public List<FriendRequestVO> listFriendRequests() {
        return contactService.listFriendRequests(UserContextHolder.getUserId());
    }

    /**
     * 发送好友申请。
     */
    @PostMapping("/request/send")
    public FriendRequestVO sendFriendRequest(@RequestBody FriendRequestCreateRequest request) {
        return contactService.sendFriendRequest(UserContextHolder.getUserId(), request);
    }

    /**
     * 处理好友申请。
     */
    @PostMapping("/request/handle")
    public FriendRequestVO handleFriendRequest(@RequestBody FriendRequestHandleRequest request) {
        return contactService.handleFriendRequest(UserContextHolder.getUserId(), request);
    }
}
