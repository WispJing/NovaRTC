package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryVO;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.service.ChatService;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImGroupMemberService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                return vo;
            }).toList();

            result.addAll(groupVos);
        }

        return result;
    }

    @Override
    public ChatHistoryPageVO history(String chatId, Long beforeChatSeq, Integer limit) {
        String currentUserId = UserContextHolder.getUserId();
        if (StringUtils.isBlank(chatId)) {
            throw new ApplicationException("chatId is blank!");
        }

        int pageSize = normalizeLimit(limit);
        validateChatAccess(chatId, currentUserId);

        List<IMMessage> messages = imMessageRepository.findHistory(chatId, beforeChatSeq, pageSize + 1);
        boolean hasMore = messages.size() > pageSize;
        List<IMMessage> pageMessages = hasMore ? messages.subList(0, pageSize) : messages;

        ChatHistoryPageVO page = new ChatHistoryPageVO();
        page.setChatId(chatId);
        page.setHasMore(hasMore);
        page.setRecords(pageMessages.stream().map(this::toHistoryVO).toList());
        if (!pageMessages.isEmpty()) {
            page.setNextBeforeChatSeq(pageMessages.get(pageMessages.size() - 1).getChatSeq());
        }
        return page;
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

    private void validateChatAccess(String chatId, String currentUserId) {
        long singleCount = imSingleChatService.count(
                new LambdaQueryWrapper<ImSingleChat>()
                        .eq(ImSingleChat::getChatId, chatId)
                        .eq(ImSingleChat::getIsDeleted, false)
                        .and(w -> w.eq(ImSingleChat::getUser1Id, currentUserId)
                                .or()
                                .eq(ImSingleChat::getUser2Id, currentUserId))
        );
        if (singleCount > 0) {
            return;
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
    }

    private ChatHistoryVO toHistoryVO(IMMessage message) {
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
        return vo;
    }
}
