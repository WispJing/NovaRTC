package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.common.enums.FriendRequestStatus;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.server.dto.FriendRequestCreateRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestHandleRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriend;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriendRequest;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.mapper.ImFriendRequestMapper;
import io.github.xiaotang.novartc.server.service.FriendRequestNotificationService;
import io.github.xiaotang.novartc.server.service.IImFriendRequestService;
import io.github.xiaotang.novartc.server.service.IImFriendService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImFriendRequestServiceImpl extends ServiceImpl<ImFriendRequestMapper, ImFriendRequest> implements IImFriendRequestService {
    private final IImUserService imUserService;
    private final IImFriendService imFriendService;
    private final IImSingleChatService imSingleChatService;
    private final FriendRequestNotificationService friendRequestNotificationService;

    @Override
    public List<FriendRequestVO> listByUserId(String currentUserId) {
        if (StringUtils.isBlank(currentUserId)) {
            return List.of();
        }
        return list(new LambdaQueryWrapper<ImFriendRequest>()
                        .eq(ImFriendRequest::getIsDeleted, false)
                        .and(wrapper -> wrapper.eq(ImFriendRequest::getRequesterId, currentUserId)
                                .or()
                                .eq(ImFriendRequest::getReceiverId, currentUserId))
                        .orderByDesc(ImFriendRequest::getUpdatedAt))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendRequestVO sendRequest(String currentUserId, FriendRequestCreateRequest request) {
        if (request == null || StringUtils.isBlank(request.getTargetUserId())) {
            throw new ApplicationException("target user is required");
        }
        if (StringUtils.equals(currentUserId, request.getTargetUserId())) {
            throw new ApplicationException("cannot add self");
        }

        ImUser targetUser = imUserService.selectByUserId(request.getTargetUserId());
        if (targetUser == null) {
            throw new ApplicationException("target user not found");
        }
        if (isFriend(currentUserId, targetUser.getId())) {
            throw new ApplicationException("already friends");
        }

        ImFriendRequest reversePending = getRequest(targetUser.getId(), currentUserId);
        if (reversePending != null && isPending(reversePending)) {
            throw new ApplicationException("target user has already sent you a friend request");
        }

        ImFriendRequest existing = getRequest(currentUserId, targetUser.getId());
        ImFriendRequest entity = existing == null ? new ImFriendRequest() : existing;
        if (existing != null && isPending(existing)) {
            throw new ApplicationException("friend request already sent");
        }

        if (StringUtils.isBlank(entity.getId())) {
            entity.setId(uuid32());
        }
        entity.setRequesterId(currentUserId);
        entity.setReceiverId(targetUser.getId());
        entity.setStatus(FriendRequestStatus.PENDING.name());
        entity.setRequestMsg(StringUtils.defaultString(request.getRequestMsg()));
        entity.setHandledAt(null);
        entity.setHandlerId(null);
        entity.setIsDeleted(false);
        saveOrUpdate(entity);

        ImFriendRequest latest = getById(entity.getId());
        friendRequestNotificationService.notifyCreated(latest);
        return toVO(latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendRequestVO handleRequest(String currentUserId, FriendRequestHandleRequest request) {
        if (request == null || StringUtils.isAnyBlank(request.getRequestId(), request.getStatus())) {
            throw new ApplicationException("requestId and status are required");
        }

        FriendRequestStatus targetStatus = parseHandleStatus(request.getStatus());
        ImFriendRequest entity = getById(request.getRequestId());
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new ApplicationException("friend request not found");
        }
        if (!isPending(entity)) {
            throw new ApplicationException("friend request has already been handled");
        }

        if (targetStatus == FriendRequestStatus.CANCELED) {
            if (!StringUtils.equals(currentUserId, entity.getRequesterId())) {
                throw new ApplicationException("only requester can cancel the friend request");
            }
        } else if (!StringUtils.equals(currentUserId, entity.getReceiverId())) {
            throw new ApplicationException("only receiver can handle the friend request");
        }

        entity.setStatus(targetStatus.name());
        entity.setHandledAt(LocalDateTime.now());
        entity.setHandlerId(currentUserId);
        updateById(entity);

        if (targetStatus == FriendRequestStatus.ACCEPTED) {
            createFriendRelation(entity);
            imSingleChatService.createOrGet(entity.getRequesterId(), entity.getReceiverId());
        }

        ImFriendRequest latest = getById(entity.getId());
        friendRequestNotificationService.notifyUpdated(latest);
        return toVO(latest);
    }

    private FriendRequestStatus parseHandleStatus(String status) {
        FriendRequestStatus targetStatus;
        try {
            targetStatus = FriendRequestStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException("invalid friend request status");
        }

        if (targetStatus == FriendRequestStatus.PENDING) {
            throw new ApplicationException("friend request status can not be pending");
        }
        return targetStatus;
    }

    private void createFriendRelation(ImFriendRequest request) {
        saveFriendRelation(request.getRequesterId(), request.getReceiverId(), request.getId());
        saveFriendRelation(request.getReceiverId(), request.getRequesterId(), request.getId());
    }

    private void saveFriendRelation(String userId, String friendId, String sourceRequestId) {
        long exists = imFriendService.count(new LambdaQueryWrapper<ImFriend>()
                .eq(ImFriend::getIsDeleted, false)
                .eq(ImFriend::getUserId, userId)
                .eq(ImFriend::getFriendId, friendId));
        if (exists > 0) {
            return;
        }

        ImFriend friend = new ImFriend();
        friend.setId(uuid32());
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setSourceRequestId(sourceRequestId);
        friend.setIsDeleted(false);
        imFriendService.save(friend);
    }

    private boolean isFriend(String userId, String targetUserId) {
        return imFriendService.count(new LambdaQueryWrapper<ImFriend>()
                .eq(ImFriend::getIsDeleted, false)
                .eq(ImFriend::getUserId, userId)
                .eq(ImFriend::getFriendId, targetUserId)) > 0;
    }

    private boolean isPending(ImFriendRequest request) {
        return StringUtils.equals(request.getStatus(), FriendRequestStatus.PENDING.name());
    }

    private ImFriendRequest getRequest(String requesterId, String receiverId) {
        if (StringUtils.isAnyBlank(requesterId, receiverId)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<ImFriendRequest>()
                .eq(ImFriendRequest::getIsDeleted, false)
                .eq(ImFriendRequest::getRequesterId, requesterId)
                .eq(ImFriendRequest::getReceiverId, receiverId)
                .last("limit 1"));
    }

    private FriendRequestVO toVO(ImFriendRequest request) {
        FriendRequestVO vo = new FriendRequestVO();
        vo.setRequestId(request.getId());
        vo.setStatus(request.getStatus());
        vo.setRequestMsg(request.getRequestMsg());
        vo.setHandledAt(request.getHandledAt());
        vo.setHandlerId(request.getHandlerId());
        vo.setCreatedAt(request.getCreatedAt());
        vo.setUpdatedAt(request.getUpdatedAt());
        fillUserInfo(vo, request.getRequesterId(), true);
        fillUserInfo(vo, request.getReceiverId(), false);
        return vo;
    }

    private void fillUserInfo(FriendRequestVO vo, String userId, boolean requester) {
        ImUser user = imUserService.selectByUserId(userId);
        if (requester) {
            vo.setRequesterId(userId);
            vo.setRequesterUserNum(user == null ? "" : user.getUserNum());
            vo.setRequesterUsername(user == null ? "" : user.getUsername());
            vo.setRequesterAvatarUrl(user == null ? "" : user.getAvatarUrl());
            return;
        }

        vo.setReceiverId(userId);
        vo.setReceiverUserNum(user == null ? "" : user.getUserNum());
        vo.setReceiverUsername(user == null ? "" : user.getUsername());
        vo.setReceiverAvatarUrl(user == null ? "" : user.getAvatarUrl());
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
