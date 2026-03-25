package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.enums.ExceptionEnum;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
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
import io.github.xiaotang.novartc.server.service.ContactService;
import io.github.xiaotang.novartc.server.service.IImFriendRequestService;
import io.github.xiaotang.novartc.server.service.IImFriendService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactServiceImpl implements ContactService {
    private final IImUserService imUserService;
    private final IImFriendService imFriendService;
    private final IImSingleChatService imSingleChatService;
    private final IImFriendRequestService imFriendRequestService;
    private final UserChannelManager userChannelManager;

    @Override
    public List<ContactVO> listContacts(String currentUserId) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatInfoVO startChat(String currentUserId, StartChatRequest request) {
        ImUser targetUser = imUserService.selectByUserId(request.getTargetUserId());
        if (targetUser == null) {
            throw ex(ExceptionEnum.FRIEND_TARGET_NOT_FOUND);
        }

        boolean isSelfChat = currentUserId.equals(targetUser.getId());
        if (!isSelfChat) {
            boolean isFriend = imFriendService.count(new LambdaQueryWrapper<ImFriend>()
                    .eq(ImFriend::getIsDeleted, false)
                    .eq(ImFriend::getUserId, currentUserId)
                    .eq(ImFriend::getFriendId, targetUser.getId())) > 0;
            if (!isFriend) {
                throw ex(ExceptionEnum.FRIEND_CHAT_ONLY_FOR_FRIENDS);
            }
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

    @Override
    public List<FriendRequestVO> listFriendRequests(String currentUserId) {
        return imFriendRequestService.listByUserId(currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendRequestVO sendFriendRequest(String currentUserId, FriendRequestCreateRequest request) {
        return imFriendRequestService.sendRequest(currentUserId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendRequestVO handleFriendRequest(String currentUserId, FriendRequestHandleRequest request) {
        return imFriendRequestService.handleRequest(currentUserId, request);
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

    private ApplicationException ex(ExceptionEnum exceptionEnum) {
        return new ApplicationException(exceptionEnum.getValue(), exceptionEnum.getContent());
    }
}


