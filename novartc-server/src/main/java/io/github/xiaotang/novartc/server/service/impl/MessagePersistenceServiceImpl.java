package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.common.util.MessageIdUtil;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.service.ChatSeqService;
import io.github.xiaotang.novartc.server.service.MessagePersistenceService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息持久化服务实现，负责写入消息存储并补全服务端生成字段。
 */
@Service
@RequiredArgsConstructor
public class MessagePersistenceServiceImpl implements MessagePersistenceService {
    private final IMMessageRepository imMessageRepository;
    private final ChatSeqService chatSeqService;
    private final MessageIdUtil messageIdUtil;

    /**
     * 持久化消息并返回补全消息编号、发送人等信息后的消息体。
     */
    @Override
    public ChatMessage.MsgBody persist(ChatMessage.MsgBody message, String authenticatedUserId) {
        String chatId = resolveChatId(message);
        long chatSeq = chatSeqService.nextSeq(chatId);
        Instant serverTime = Instant.now();
        String messageId = StringUtils.defaultIfBlank(message.getMessageId(), String.valueOf(messageIdUtil.nextId()));

        ChatMessage.MsgBody persistedMessage = rebuildMessage(message, authenticatedUserId, messageId);
        imMessageRepository.insert(toEntity(persistedMessage, chatSeq, serverTime));
        return persistedMessage;
    }

    private IMMessage toEntity(ChatMessage.MsgBody message, long chatSeq, Instant serverTime) {
        IMMessage imMessage = new IMMessage();
        imMessage.setMessageId(message.getMessageId());
        imMessage.setChatSeq(chatSeq);
        imMessage.setChatId(resolveChatId(message));
        imMessage.setSenderId(message.getSender().getUserId());
        imMessage.setSenderName(message.getSender().getUsername());
        imMessage.setSenderAvatarUrl(message.getSender().getAvatarUrl());
        imMessage.setIsGroup(message.getIsGroup());
        imMessage.setMessageType(message.getMessageType().name());
        imMessage.setMessageText(extractMessageText(message));
        imMessage.setContentJson(JacksonUtil.toJson(buildContentMap(message)));
        imMessage.setServerTime(serverTime);
        imMessage.setClientTime(resolveClientTime(message));
        imMessage.setChatType(message.getIsGroup() ? 2 : 1);

        if (message.getIsGroup()) {
            imMessage.setGroupId(message.getGroupChatInfo().getGroupId());
        } else {
            imMessage.setTargetUserId(resolveTargetUserId(message));
        }
        return imMessage;
    }

    private ChatMessage.MsgBody rebuildMessage(ChatMessage.MsgBody message,
                                               String authenticatedUserId,
                                               String messageId) {
        ChatMessage.User sender = message.getSender().toBuilder()
                .setUserId(authenticatedUserId)
                .build();

        return message.toBuilder()
                .setMessageId(messageId)
                .setSender(sender)
                .build();
    }

    private Instant resolveClientTime(ChatMessage.MsgBody message) {
        if (message.getTimestamp() <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(message.getTimestamp());
    }

    private String resolveChatId(ChatMessage.MsgBody message) {
        String chatId = message.getIsGroup()
                ? message.getGroupChatInfo().getChatId()
                : message.getSingleChatInfo().getChatId();
        if (StringUtils.isBlank(chatId)) {
            throw new ApplicationException("会话标识不能为空");
        }
        return chatId;
    }

    private String resolveTargetUserId(ChatMessage.MsgBody message) {
        String senderId = message.getSender().getUserId();
        String user1Id = message.getSingleChatInfo().getUser1Id();
        String user2Id = message.getSingleChatInfo().getUser2Id();
        if (StringUtils.equals(senderId, user1Id)) {
            return user2Id;
        }
        return user1Id;
    }

    private String extractMessageText(ChatMessage.MsgBody message) {
        return switch (message.getContentCase()) {
            case TEXT_CONTENT -> message.getTextContent().getText();
            case TEXT_AND_IMAGE_CONTENT -> message.getTextAndImageContent().getTextContent().getText();
            default -> null;
        };
    }

    private Map<String, Object> buildContentMap(ChatMessage.MsgBody message) {
        Map<String, Object> content = new LinkedHashMap<>();
        switch (message.getContentCase()) {
            case TEXT_CONTENT -> content.put("text", message.getTextContent().getText());
            case IMAGE_CONTENT -> {
                content.put("imageUrl", message.getImageContent().getImageUrl());
                content.put("imageDescription", message.getImageContent().getImageDescription());
            }
            case TEXT_AND_IMAGE_CONTENT -> {
                content.put("text", message.getTextAndImageContent().getTextContent().getText());
                content.put("imageUrl", message.getTextAndImageContent().getImageContent().getImageUrl());
                content.put("imageDescription", message.getTextAndImageContent().getImageContent().getImageDescription());
            }
            case FILE_CONTENT -> {
                content.put("fileUrl", message.getFileContent().getFileUrl());
                content.put("fileName", message.getFileContent().getFileName());
                content.put("fileSize", message.getFileContent().getFileSize());
                content.put("contentType", message.getFileContent().getContentType());
            }
            case VIDEO_CONTENT -> {
                content.put("videoUrl", message.getVideoContent().getVideoUrl());
                content.put("fileName", message.getVideoContent().getFileName());
                content.put("fileSize", message.getVideoContent().getFileSize());
            }
            case AUDIO_CONTENT -> {
                content.put("audioUrl", message.getAudioContent().getAudioUrl());
                content.put("fileName", message.getAudioContent().getFileName());
                content.put("fileSize", message.getAudioContent().getFileSize());
                content.put("durationSeconds", message.getAudioContent().getDurationSeconds());
            }
            default -> {
            }
        }
        return content;
    }
}
