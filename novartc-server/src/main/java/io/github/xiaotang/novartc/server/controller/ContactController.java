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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactService contactService;

    @PostMapping("/list")
    public List<ContactVO> list() {
        return contactService.listContacts(UserContextHolder.getUserId());
    }

    @PostMapping("/start")
    public ChatInfoVO start(@RequestBody StartChatRequest request) {
        return contactService.startChat(UserContextHolder.getUserId(), request);
    }

    @PostMapping("/request/list")
    public List<FriendRequestVO> listFriendRequests() {
        return contactService.listFriendRequests(UserContextHolder.getUserId());
    }

    @PostMapping("/request/send")
    public FriendRequestVO sendFriendRequest(@RequestBody FriendRequestCreateRequest request) {
        return contactService.sendFriendRequest(UserContextHolder.getUserId(), request);
    }

    @PostMapping("/request/handle")
    public FriendRequestVO handleFriendRequest(@RequestBody FriendRequestHandleRequest request) {
        return contactService.handleFriendRequest(UserContextHolder.getUserId(), request);
    }
}
