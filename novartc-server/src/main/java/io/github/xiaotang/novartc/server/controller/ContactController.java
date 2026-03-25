package io.github.xiaotang.novartc.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ContactVO;
import io.github.xiaotang.novartc.server.dto.FriendRequestCreateRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestHandleRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.dto.StartChatRequest;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriend;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.service.IImFriendRequestService;
import io.github.xiaotang.novartc.server.service.IImFriendService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final IImUserService imUserService;
    private final IImFriendService imFriendService;
    private final IImSingleChatService imSingleChatService;
    private final IImFriendRequestService imFriendRequestService;
    private final UserChannelManager userChannelManager;

    @PostMapping("/list")
    public List<ContactVO> list() {
        String currentUserId = UserContextHolder.getUserId();
        Set<String> friendUserIds = imFriendService.list(new LambdaQueryWrapper<ImFriend>()
                        .eq(ImFriend::getIsDeleted, false)
                        .eq(ImFriend::getUserId, currentUserId))
                .stream()
                .map(ImFriend::getFriendId)
                .filter(friendId -> friendId != null && !friendId.isBlank())
                .collect(Collectors.toSet());

        if (friendUserIds.isEmpty()) {
            return List.of();
        }

        return imUserService.list(new LambdaQueryWrapper<ImUser>()
                        .eq(ImUser::getIsDeleted, false)
                        .in(ImUser::getId, friendUserIds)
                        .orderByAsc(ImUser::getUserNum))
                .stream()
                .map(this::toContactVO)
                .toList();
    }

    @PostMapping("/start")
    public ChatInfoVO start(@RequestBody StartChatRequest request) {
        String currentUserId = UserContextHolder.getUserId();
        ImUser targetUser = imUserService.selectByUserId(request.getTargetUserId());
        if (targetUser == null) {
            throw new ApplicationException("target user not found");
        }

        boolean isFriend = imFriendService.count(new LambdaQueryWrapper<ImFriend>()
                .eq(ImFriend::getIsDeleted, false)
                .eq(ImFriend::getUserId, currentUserId)
                .eq(ImFriend::getFriendId, targetUser.getId())) > 0;
        if (!isFriend) {
            throw new ApplicationException("can only start chat with friends");
        }

        ImSingleChat singleChat = imSingleChatService.createOrGet(currentUserId, targetUser.getId());

        ChatInfoVO vo = new ChatInfoVO();
        vo.setChatType("single");
        vo.setChatId(singleChat.getChatId());
        vo.setTargetUserId(targetUser.getId());
        vo.setTargetUserNum(targetUser.getUserNum());
        vo.setTitle(targetUser.getUsername());
        vo.setAvatarUrl(targetUser.getAvatarUrl());
        vo.setOnline(userChannelManager.isOnline(targetUser.getId()));
        vo.setLastMessage("");
        vo.setLastMessageType("SYSTEM");
        vo.setUnreadCount(0);
        return vo;
    }

    @PostMapping("/request/list")
    public List<FriendRequestVO> listFriendRequests() {
        return imFriendRequestService.listByUserId(UserContextHolder.getUserId());
    }

    @PostMapping("/request/send")
    public FriendRequestVO sendFriendRequest(@RequestBody FriendRequestCreateRequest request) {
        return imFriendRequestService.sendRequest(UserContextHolder.getUserId(), request);
    }

    @PostMapping("/request/handle")
    public FriendRequestVO handleFriendRequest(@RequestBody FriendRequestHandleRequest request) {
        return imFriendRequestService.handleRequest(UserContextHolder.getUserId(), request);
    }

    private ContactVO toContactVO(ImUser user) {
        ContactVO vo = new ContactVO();
        vo.setUserId(user.getId());
        vo.setUserNum(user.getUserNum());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setOnline(userChannelManager.isOnline(user.getId()));
        return vo;
    }
}
