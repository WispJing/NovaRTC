package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.enums.FriendRequestStatus;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriendRequest;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import io.github.xiaotang.novartc.server.util.MinioUrlResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 好友申请通知服务，负责将申请创建和处理事件分发给相关用户。
 */
@Service
@RequiredArgsConstructor
public class FriendRequestNotificationService {
    private final ChatDispatchService chatDispatchService;
    private final IImUserService imUserService;
    private final MinioUrlResolver minioUrlResolver;

    /**
     * 推送好友申请创建事件。
     */
    public void notifyCreated(ImFriendRequest request) {
        dispatch(request, request.getRequesterId(), request.getReceiverId(), ChatMessage.FriendRequestEventType.FRIEND_REQUEST_CREATED, true);
    }

    /**
     * 推送好友申请状态更新事件。
     */
    public void notifyUpdated(ImFriendRequest request) {
        String actorUserId = StringUtils.defaultIfBlank(request.getHandlerId(), request.getRequesterId());
        Set<String> targetUserIds = new LinkedHashSet<>();
        targetUserIds.add(request.getRequesterId());
        targetUserIds.add(request.getReceiverId());
        for (String targetUserId : targetUserIds) {
            dispatch(request, actorUserId, targetUserId, ChatMessage.FriendRequestEventType.FRIEND_REQUEST_UPDATED, true);
        }
    }

    private void dispatch(ImFriendRequest request, String actorUserId, String targetUserId,
                          ChatMessage.FriendRequestEventType eventType, boolean unread) {
        if (StringUtils.isAnyBlank(actorUserId, targetUserId)) {
            return;
        }

        ImUser actorUser = imUserService.selectByUserId(actorUserId);
        ChatMessage.MsgBody message = ChatMessage.MsgBody.newBuilder()
                .setMessageId("friend-request-" + request.getId() + "-" + targetUserId + "-" + System.currentTimeMillis())
                .setMessageType(ChatMessage.MessageType.FRIEND_REQUEST)
                .setTimestamp(System.currentTimeMillis())
                .setIsGroup(false)
                .setSender(buildSender(actorUserId, actorUser))
                .setSingleChatInfo(ChatMessage.SingleChatInfo.newBuilder()
                        .setChatId("")
                        .setUser1Id(StringUtils.defaultString(actorUserId))
                        .setUser2Id(StringUtils.defaultString(targetUserId))
                        .build())
                .setFriendRequestEvent(buildEvent(request, eventType, unread))
                .build();
        chatDispatchService.dispatch(message);
    }

    private ChatMessage.User buildSender(String actorUserId, ImUser actorUser) {
        return ChatMessage.User.newBuilder()
                .setUserId(StringUtils.defaultString(actorUserId))
                .setUsername(actorUser == null ? "" : StringUtils.defaultString(actorUser.getUsername()))
                .setAvatarUrl(actorUser == null ? ""
                        : StringUtils.defaultString(minioUrlResolver.resolve(actorUser.getAvatarUrl())))
                .build();
    }

    private ChatMessage.FriendRequestEvent buildEvent(ImFriendRequest request,
                                                      ChatMessage.FriendRequestEventType eventType,
                                                      boolean unread) {
        return ChatMessage.FriendRequestEvent.newBuilder()
                .setRequestId(StringUtils.defaultString(request.getId()))
                .setRequesterId(StringUtils.defaultString(request.getRequesterId()))
                .setReceiverId(StringUtils.defaultString(request.getReceiverId()))
                .setStatus(toProtoStatus(request.getStatus()))
                .setRequestMsg(StringUtils.defaultString(request.getRequestMsg()))
                .setCreatedAt(request.getCreatedAt() == null ? 0L : request.getCreatedAt().toEpochMilli())
                .setHandledAt(toEpochMillis(request.getHandledAt()))
                .setHandlerId(StringUtils.defaultString(request.getHandlerId()))
                .setEventType(eventType)
                .setUnread(unread)
                .build();
    }

    private ChatMessage.FriendRequestStatus toProtoStatus(String status) {
        FriendRequestStatus requestStatus;
        try {
            requestStatus = FriendRequestStatus.valueOf(StringUtils.defaultIfBlank(status, FriendRequestStatus.PENDING.name()));
        } catch (IllegalArgumentException ex) {
            requestStatus = FriendRequestStatus.PENDING;
        }

        return switch (requestStatus) {
            case ACCEPTED -> ChatMessage.FriendRequestStatus.FRIEND_REQUEST_ACCEPTED;
            case REJECTED -> ChatMessage.FriendRequestStatus.FRIEND_REQUEST_REJECTED;
            case CANCELED -> ChatMessage.FriendRequestStatus.FRIEND_REQUEST_CANCELED;
            case PENDING -> ChatMessage.FriendRequestStatus.FRIEND_REQUEST_PENDING;
        };
    }

    private long toEpochMillis(Instant value) {
        if (value == null) {
            return 0L;
        }
        return value.toEpochMilli();
    }
}
