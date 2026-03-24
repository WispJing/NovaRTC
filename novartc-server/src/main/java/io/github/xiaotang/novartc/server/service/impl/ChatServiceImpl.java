package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryVO;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.entity.mongodb.IMReadStatus;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.repository.mongodb.IMReadStatusRepository;
import io.github.xiaotang.novartc.server.service.ChatService;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImGroupMemberService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created on 2026/3/20 11:35
 *
 * @author tangj
 * @description
 */
@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {
    private final IImUserService imUserService;
    private final IImSingleChatService imSingleChatService;
    private final IImGroupChatService imGroupChatService;
    private final IImGroupMemberService imGroupMemberService;
    private final IMMessageRepository imMessageRepository;
    private final IMReadStatusRepository imReadStatusRepository;

    @Override
    public List<ChatInfoVO> chatList() {
        String currentUserId = UserContextHolder.getUserId();

        List<ImSingleChat> singleChats = imSingleChatService.list(
                new LambdaQueryWrapper<ImSingleChat>()
                        .eq(ImSingleChat::getIsDeleted, false)
                        .and(w -> w.eq(ImSingleChat::getUser1Id, currentUserId)
                                .or()
                                .eq(ImSingleChat::getUser2Id, currentUserId))
        );

        Set<String> targetUserIds = singleChats.stream()
                .map(chat -> chat.getUser1Id().equals(currentUserId) ? chat.getUser2Id() : chat.getUser1Id())
                .collect(Collectors.toSet());

        Map<String, ImUser> userMap = targetUserIds.isEmpty()
                ? Map.of()
                : imUserService.list(
                new LambdaQueryWrapper<ImUser>()
                        .in(ImUser::getId, targetUserIds)
                        .eq(ImUser::getIsDeleted, false)
        ).stream().collect(Collectors.toMap(ImUser::getId, Function.identity()));

        List<ChatInfoVO> singleVos = singleChats.stream().map(chat -> {
            String targetUserId = chat.getUser1Id().equals(currentUserId)
                    ? chat.getUser2Id()
                    : chat.getUser1Id();

            ImUser targetUser = userMap.get(targetUserId);

            ChatInfoVO vo = new ChatInfoVO();
            vo.setChatType("single");
            vo.setChatId(chat.getChatId());
            vo.setTargetUserId(targetUserId);

            if (targetUser != null) {
                vo.setTitle(targetUser.getUsername());
                vo.setTargetUserNum(targetUser.getUserNum());
                vo.setAvatarUrl(targetUser.getAvatarUrl());
            } else {
                vo.setTitle("未知用户");
            }

            fillLastMessage(vo, currentUserId);
            vo.setUnreadCount(countUnreadMessages(vo.getChatId(), currentUserId));
            return vo;
        }).toList();

        List<ChatInfoVO> result = new ArrayList<>(singleVos);

        List<ImGroupMember> members = imGroupMemberService.list(
                new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getUserId, currentUserId)
                        .eq(ImGroupMember::getIsDeleted, false)
        );

        List<String> groupIds = members.stream()
                .map(ImGroupMember::getGroupId)
                .toList();

        if (!groupIds.isEmpty()) {
            List<ImGroupChat> groupChats = imGroupChatService.list(
                    new LambdaQueryWrapper<ImGroupChat>()
                            .in(ImGroupChat::getId, groupIds)
                            .eq(ImGroupChat::getIsDeleted, false)
            );

            List<ChatInfoVO> groupVos = groupChats.stream().map(group -> {
                ChatInfoVO vo = new ChatInfoVO();
                vo.setChatType("group");
                vo.setChatId(group.getChatId());
                vo.setGroupId(group.getId());
                vo.setGroupNum(group.getGroupNum());
                vo.setTitle(group.getGroupName());
                fillLastMessage(vo, currentUserId);
                vo.setUnreadCount(countUnreadMessages(vo.getChatId(), currentUserId));
                return vo;
            }).toList();

            result.addAll(groupVos);
        }

        return result.stream()
                .sorted((left, right) -> {
                    if (left.getLastMessageTime() == null && right.getLastMessageTime() == null) {
                        return 0;
                    }
                    if (left.getLastMessageTime() == null) {
                        return 1;
                    }
                    if (right.getLastMessageTime() == null) {
                        return -1;
                    }
                    return right.getLastMessageTime().compareTo(left.getLastMessageTime());
                })
                .toList();
    }

    @Override
    public ChatHistoryPageVO history(String chatId, Long beforeChatSeq, Integer limit) {
        String currentUserId = UserContextHolder.getUserId();
        if (StringUtils.isBlank(chatId)) {
            throw new ApplicationException("chatId is blank!");
        }

        int pageSize = normalizeLimit(limit);
        ImSingleChat singleChat = validateChatAccess(chatId, currentUserId);
        markRead(chatId);

        List<IMMessage> messages = imMessageRepository.findHistory(chatId, beforeChatSeq, pageSize + 1);
        boolean hasMore = messages.size() > pageSize;
        List<IMMessage> pageMessages = hasMore ? messages.subList(0, pageSize) : messages;
        List<String> pageMessageIds = pageMessages.stream()
                .map(IMMessage::getMessageId)
                .filter(StringUtils::isNotBlank)
                .toList();

        Set<String> currentUserReadIds = imReadStatusRepository.findReadMessageIds(currentUserId, pageMessageIds);
        String peerUserId = singleChat == null ? null : resolvePeerUserId(singleChat, currentUserId);
        Set<String> peerReadIds = StringUtils.isBlank(peerUserId)
                ? Set.of()
                : imReadStatusRepository.findReadMessageIds(peerUserId, pageMessageIds);

        ChatHistoryPageVO page = new ChatHistoryPageVO();
        page.setChatId(chatId);
        page.setHasMore(hasMore);
        page.setRecords(pageMessages.stream()
                .map(message -> toHistoryVO(message, currentUserId, peerReadIds, currentUserReadIds))
                .toList());
        if (!pageMessages.isEmpty()) {
            page.setNextBeforeChatSeq(pageMessages.get(pageMessages.size() - 1).getChatSeq());
        }
        return page;
    }

    @Override
    public void markRead(String chatId) {
        String currentUserId = UserContextHolder.getUserId();
        if (StringUtils.isBlank(chatId)) {
            throw new ApplicationException("chatId is blank!");
        }

        validateChatAccess(chatId, currentUserId);

        List<String> unreadMessageIds = imMessageRepository.findOtherUserMessageIds(chatId, currentUserId);
        if (unreadMessageIds.isEmpty()) {
            return;
        }

        Set<String> existingReadIds = imReadStatusRepository.findReadMessageIds(currentUserId, unreadMessageIds);
        List<IMReadStatus> inserts = unreadMessageIds.stream()
                .filter(messageId -> !existingReadIds.contains(messageId))
                .map(messageId -> createReadStatus(currentUserId, messageId))
                .toList();

        if (!inserts.isEmpty()) {
            imReadStatusRepository.insertBatch(inserts);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        if (limit < 1 || limit > 100) {
            throw new ApplicationException("limit must be between 1 and 100");
        }
        return limit;
    }

    private ImSingleChat validateChatAccess(String chatId, String currentUserId) {
        ImSingleChat singleChat = imSingleChatService.getOne(
                new LambdaQueryWrapper<ImSingleChat>()
                        .eq(ImSingleChat::getChatId, chatId)
                        .eq(ImSingleChat::getIsDeleted, false)
                        .and(w -> w.eq(ImSingleChat::getUser1Id, currentUserId)
                                .or()
                                .eq(ImSingleChat::getUser2Id, currentUserId))
                        .last("limit 1")
        );
        if (singleChat != null) {
            return singleChat;
        }

        ImGroupChat groupChat = imGroupChatService.getOne(
                new LambdaQueryWrapper<ImGroupChat>()
                        .eq(ImGroupChat::getChatId, chatId)
                        .eq(ImGroupChat::getIsDeleted, false)
                        .last("limit 1")
        );
        if (groupChat == null) {
            throw new ApplicationException("chat not found");
        }

        long memberCount = imGroupMemberService.count(
                new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getGroupId, groupChat.getId())
                        .eq(ImGroupMember::getUserId, currentUserId)
                        .eq(ImGroupMember::getIsDeleted, false)
        );
        if (memberCount <= 0) {
            throw new ApplicationException("no permission to access this chat");
        }
        return null;
    }

    private ChatHistoryVO toHistoryVO(IMMessage message,
                                      String currentUserId,
                                      Set<String> peerReadIds,
                                      Set<String> currentUserReadIds) {
        ChatHistoryVO vo = new ChatHistoryVO();
        vo.setMessageId(message.getMessageId());
        vo.setChatSeq(message.getChatSeq());
        vo.setChatId(message.getChatId());
        vo.setIsGroup(message.getIsGroup());
        vo.setGroupId(message.getGroupId());
        vo.setTargetUserId(message.getTargetUserId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderName(message.getSenderName());
        vo.setSenderAvatarUrl(message.getSenderAvatarUrl());
        vo.setMessageType(message.getMessageType());
        vo.setMessageText(message.getMessageText());
        vo.setServerTime(message.getServerTime());
        vo.setClientTime(message.getClientTime());
        if (StringUtils.isNotBlank(message.getContentJson())) {
            vo.setContent(JacksonUtil.toObject(message.getContentJson(), Object.class));
        }

        if (StringUtils.equals(message.getSenderId(), currentUserId)) {
            vo.setRead(Boolean.TRUE.equals(message.getIsGroup()) ? Boolean.FALSE : peerReadIds.contains(message.getMessageId()));
        } else {
            vo.setRead(currentUserReadIds.contains(message.getMessageId()));
        }
        return vo;
    }

    private void fillLastMessage(ChatInfoVO vo, String currentUserId) {
        List<IMMessage> messages = imMessageRepository.findHistory(vo.getChatId(), null, 1);
        if (messages.isEmpty()) {
            return;
        }

        IMMessage latest = messages.getFirst();
        vo.setLastMessageType(latest.getMessageType());
        vo.setLastMessageTime(latest.getServerTime() != null ? latest.getServerTime() : latest.getClientTime());
        vo.setLastMessage(resolveLastMessage(latest));
        if (vo.getUnreadCount() == null) {
            vo.setUnreadCount(countUnreadMessages(vo.getChatId(), currentUserId));
        }
    }

    private int countUnreadMessages(String chatId, String currentUserId) {
        List<String> otherUserMessageIds = imMessageRepository.findOtherUserMessageIds(chatId, currentUserId);
        if (otherUserMessageIds.isEmpty()) {
            return 0;
        }

        Set<String> readMessageIds = imReadStatusRepository.findReadMessageIds(currentUserId, otherUserMessageIds);
        return otherUserMessageIds.size() - readMessageIds.size();
    }

    private IMReadStatus createReadStatus(String currentUserId, String messageId) {
        IMReadStatus readStatus = new IMReadStatus();
        String id = uuid32();
        readStatus.setId(id);
        readStatus.setRead_status_id(id);
        readStatus.setMessage_id(messageId);
        readStatus.setUser_id(currentUserId);
        readStatus.setRead_at(Instant.now());
        return readStatus;
    }

    private String resolvePeerUserId(ImSingleChat singleChat, String currentUserId) {
        return StringUtils.equals(singleChat.getUser1Id(), currentUserId)
                ? singleChat.getUser2Id()
                : singleChat.getUser1Id();
    }

    private String resolveLastMessage(IMMessage latest) {
        if (StringUtils.isNotBlank(latest.getMessageText())) {
            return latest.getMessageText();
        }
        return switch (StringUtils.defaultString(latest.getMessageType()).toUpperCase()) {
            case "IMAGE" -> "[图片]";
            case "TEXT_AND_IMAGE" -> "[图文]";
            case "FILE" -> "[文件]";
            case "VIDEO" -> "[视频]";
            case "AUDIO" -> "[语音]";
            default -> "[消息]";
        };
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
