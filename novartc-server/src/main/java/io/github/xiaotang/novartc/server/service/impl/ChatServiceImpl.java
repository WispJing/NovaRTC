package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.common.util.StringUtil;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryVO;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.entity.mongodb.IMReadStatus;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.repository.mongodb.IMReadStatusRepository;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import io.github.xiaotang.novartc.server.service.ChatService;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImGroupMemberService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.util.MinioUtil;
import io.github.xiaotang.novartc.server.util.MinioUrlResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * IM 会话服务实现，负责会话列表、历史消息和已读状态处理。
 */
@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    /**
     * 撤回时限（分钟）
     */
    private static final long RECALL_WINDOW_MINUTES = 2;

    private final IImUserService imUserService;
    private final IImSingleChatService imSingleChatService;
    private final IImGroupChatService imGroupChatService;
    private final IImGroupMemberService imGroupMemberService;
    private final IMMessageRepository imMessageRepository;
    private final IMReadStatusRepository imReadStatusRepository;
    private final UserRouteService userRouteService;
    private final ChatDispatchService chatDispatchService;
    private final ImFileMapper imFileMapper;
    private final MinioUtil minioUtil;
    private final MinioProperties minioProperties;
    private final MinioUrlResolver minioUrlResolver;

    /**
     * 查询当前用户的会话列表，并附带最后一条消息与未读数。
     */
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
                vo.setAvatarUrl(minioUrlResolver.resolve(targetUser.getAvatarUrl()));
                vo.setOnline(isUserOnline(targetUser.getId()));
            } else {
                vo.setTitle("未知用户");
                vo.setOnline(false);
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
            List<ImGroupMember> groupMembers = imGroupMemberService.list(
                    new LambdaQueryWrapper<ImGroupMember>()
                            .in(ImGroupMember::getGroupId, groupIds)
                            .eq(ImGroupMember::getIsDeleted, false)
            );
            Map<String, List<ImGroupMember>> membersByGroupId = groupMembers.stream()
                    .collect(Collectors.groupingBy(ImGroupMember::getGroupId));
            Map<String, Boolean> onlineUserMap = groupMembers.stream()
                    .map(ImGroupMember::getUserId)
                    .distinct()
                    .collect(Collectors.toMap(Function.identity(), this::isUserOnline));

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
                List<ImGroupMember> groupMemberList = membersByGroupId.getOrDefault(group.getId(), List.of());
                vo.setMemberCount(groupMemberList.size());
                vo.setOnlineCount((int) groupMemberList.stream()
                        .map(ImGroupMember::getUserId)
                        .filter(userId -> Boolean.TRUE.equals(onlineUserMap.get(userId)))
                        .count());
                return vo;
            }).toList();

            result.addAll(groupVos);
        }

        fillChatSummaries(result, currentUserId);
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

    /**
     * 分页查询指定会话的历史消息，并补充已读状态。
     */
    @Override
    public ChatHistoryPageVO history(String chatId, Long beforeChatSeq, Integer limit) {
        String currentUserId = UserContextHolder.getUserId();
        if (StringUtils.isBlank(chatId)) {
            throw new ApplicationException("会话标识不能为空");
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

    /**
     * 将当前用户在指定会话中的未读消息批量标记为已读。
     */
    @Override
    public void markRead(String chatId) {
        String currentUserId = UserContextHolder.getUserId();
        if (StringUtils.isBlank(chatId)) {
            throw new ApplicationException("会话标识不能为空");
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

    /**
     * 撤回当前用户 2 分钟内发送的消息：校验后删除关联文件、标记撤回并通知对端。
     */
    @Override
    public void recall(String chatId, String messageId) {
        String currentUserId = UserContextHolder.getUserId();
        if (StringUtils.isBlank(chatId) || StringUtils.isBlank(messageId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "会话和消息标识不能为空");
        }

        IMMessage message = imMessageRepository.findByMessageId(messageId);
        if (message == null || !StringUtils.equals(message.getChatId(), chatId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "消息不存在");
        }
        if (!StringUtils.equals(message.getSenderId(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.code(), "只能撤回自己的消息");
        }
        if (Boolean.TRUE.equals(message.getRecalled())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "消息已被撤回");
        }
        Instant serverTime = message.getServerTime() != null ? message.getServerTime() : Instant.EPOCH;
        if (Instant.now().isAfter(serverTime.plus(RECALL_WINDOW_MINUTES, ChronoUnit.MINUTES))) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "已超过撤回时限");
        }

        removeRecalledMessageFile(message);

        message.setRecalled(Boolean.TRUE);
        imMessageRepository.save(message);

        dispatchRecallNotice(message, currentUserId);
        logger.info("消息撤回成功: 用户={}, 会话={}, 消息={}", currentUserId, chatId, messageId);
    }

    /**
     * 删除被撤回消息关联的存储文件（MinIO 对象与 im_file 记录）。
     * 文件删除失败仅记录日志，不阻断撤回本身。
     */
    private void removeRecalledMessageFile(IMMessage message) {
        String fileUrl = resolveMessageFileUrl(message);
        if (StringUtils.isBlank(fileUrl)) {
            return;
        }
        String objectKey = minioUrlResolver.resolveObjectKey(fileUrl);
        ImFile imFile = imFileMapper.selectOne(
                new LambdaQueryWrapper<ImFile>()
                        .eq(ImFile::getUploaderId, message.getSenderId())
                        .and(wrapper -> wrapper
                                .eq(ImFile::getUrl, fileUrl)
                                .or(StringUtils.isNotBlank(objectKey),
                                        nested -> nested.eq(ImFile::getObjectKey, objectKey)))
                        .last("limit 1")
        );
        if (imFile == null) {
            logger.warn("撤回消息未找到关联文件记录: 消息={}, 文件={}", message.getMessageId(), fileUrl);
            return;
        }
        try {
            minioUtil.removeObject(minioProperties.getBucket(), imFile.getObjectKey());
        } catch (RuntimeException e) {
            logger.error("撤回消息删除文件失败: 消息={}, 对象={}", message.getMessageId(), imFile.getObjectKey(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR.code(), "文件删除失败，请稍后重试");
        }
        imFileMapper.deleteById(imFile.getId());
        logger.info("撤回消息已删除关联文件: 消息={}, 对象={}", message.getMessageId(), imFile.getObjectKey());
    }

    /**
     * 从消息内容 JSON 中提取关联文件地址（图片/文件/视频/语音任一），无则返回 null。
     */
    private String resolveMessageFileUrl(IMMessage message) {
        if (StringUtils.isBlank(message.getContentJson())) {
            return null;
        }
        Map<String, Object> content;
        try {
            content = JacksonUtil.toObject(message.getContentJson(), Map.class);
        } catch (RuntimeException e) {
            logger.warn("解析消息内容失败，跳过文件删除: 消息={}", message.getMessageId(), e);
            return null;
        }
        if (content == null) {
            return null;
        }
        for (String key : List.of("imageUrl", "fileUrl", "videoUrl", "audioUrl")) {
            Object value = content.get(key);
            if (value instanceof String url && StringUtils.isNotBlank(url)) {
                return url;
            }
        }
        return null;
    }

    /**
     * 向对端或群成员推送撤回通知帧，发送方自身由 REST 响应更新本地状态。
     */
    private void dispatchRecallNotice(IMMessage message, String operatorId) {
        ChatMessage.MsgBody.Builder builder = ChatMessage.MsgBody.newBuilder()
                .setMessageId(StringUtil.uuid32())
                .setMessageType(ChatMessage.MessageType.RECALL)
                .setTimestamp(Instant.now().toEpochMilli())
                .setIsGroup(Boolean.TRUE.equals(message.getIsGroup()))
                .setSender(ChatMessage.User.newBuilder().setUserId(operatorId).build())
                .setRecallContent(ChatMessage.RecallContent.newBuilder()
                        .setRecallMessageId(message.getMessageId())
                        .setOperatorId(operatorId)
                        .build());
        if (Boolean.TRUE.equals(message.getIsGroup())) {
            builder.setGroupChatInfo(ChatMessage.GroupChatInfo.newBuilder()
                    .setChatId(StringUtils.defaultString(message.getChatId()))
                    .setGroupId(StringUtils.defaultString(message.getGroupId()))
                    .build());
        } else {
            builder.setSingleChatInfo(ChatMessage.SingleChatInfo.newBuilder()
                    .setChatId(StringUtils.defaultString(message.getChatId()))
                    .setUser1Id(operatorId)
                    .setUser2Id(StringUtils.defaultString(message.getTargetUserId()))
                    .build());
        }
        chatDispatchService.dispatch(builder.build());
    }

    private boolean isUserOnline(String userId) {
        return !userRouteService.getUserInstances(userId).isEmpty();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        if (limit < 1 || limit > 100) {
            throw new ApplicationException("分页条数必须在 1 到 100 之间");
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
            throw new ApplicationException("未找到对应会话");
        }

        long memberCount = imGroupMemberService.count(
                new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getGroupId, groupChat.getId())
                        .eq(ImGroupMember::getUserId, currentUserId)
                        .eq(ImGroupMember::getIsDeleted, false)
        );
        if (memberCount <= 0) {
            throw new ApplicationException("无权访问该会话");
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
        vo.setRecalled(Boolean.TRUE.equals(message.getRecalled()));
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

    private void fillChatSummaries(List<ChatInfoVO> chats, String currentUserId) {
        if (chats.isEmpty()) {
            return;
        }
        List<String> chatIds = chats.stream()
                .map(ChatInfoVO::getChatId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, IMMessage> latestMessages = imMessageRepository.findLatestByChatIds(chatIds);
        Map<String, Integer> unreadCounts = countUnreadMessages(chatIds, currentUserId);
        for (ChatInfoVO chat : chats) {
            IMMessage latest = latestMessages.get(chat.getChatId());
            if (latest != null) {
                applyLastMessage(chat, latest);
            }
            chat.setUnreadCount(unreadCounts.getOrDefault(chat.getChatId(), 0));
        }
    }

    private void applyLastMessage(ChatInfoVO vo, IMMessage latest) {
        vo.setLastMessageType(Boolean.TRUE.equals(latest.getRecalled()) ? "RECALL" : latest.getMessageType());
        vo.setLastMessageTime(latest.getServerTime() != null ? latest.getServerTime() : latest.getClientTime());
        vo.setLastMessage(resolveLastMessage(latest));
    }

    private Map<String, Integer> countUnreadMessages(List<String> chatIds, String currentUserId) {
        Map<String, List<String>> messageIdsByChat = imMessageRepository
                .findOtherUserMessageIdsByChatIds(chatIds, currentUserId);
        List<String> messageIds = messageIdsByChat.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        Set<String> readMessageIds = imReadStatusRepository.findReadMessageIds(currentUserId, messageIds);
        return messageIdsByChat.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (int) entry.getValue().stream()
                        .filter(messageId -> !readMessageIds.contains(messageId))
                        .count()
        ));
    }

    private IMReadStatus createReadStatus(String currentUserId, String messageId) {
        IMReadStatus readStatus = new IMReadStatus();
        String id = StringUtil.uuid32();
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
        if (Boolean.TRUE.equals(latest.getRecalled())) {
            return "[消息已撤回]";
        }
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
}
