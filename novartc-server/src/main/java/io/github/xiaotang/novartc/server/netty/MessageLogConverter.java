package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.proto.ChatMessage;

public class MessageLogConverter {
    public static MessageLog convert(ChatMessage.MsgBody msg) {

        MessageLog.MessageLogBuilder builder = MessageLog.builder()
                .event("im_message")
                .messageId(msg.getMessageId())
                .messageType(msg.getMessageType().name())
                .timestamp(msg.getTimestamp())
                .isGroup(msg.getIsGroup())
                .senderId(msg.getSender().getUserId())
                .senderName(msg.getSender().getUsername());

        if (!msg.getIsGroup() && msg.hasSingleChatInfo()) {
            builder.chatId(msg.getSingleChatInfo().getChatId());
            builder.targetId(msg.getSingleChatInfo().getUser2Id());
        }

        if (msg.getIsGroup() && msg.hasGroupChatInfo()) {
            builder.chatId(msg.getGroupChatInfo().getChatId());
            builder.targetId(msg.getGroupChatInfo().getGroupId());
        }

        switch (msg.getContentCase()) {
            case TEXT_CONTENT -> builder.content(msg.getTextContent().getText());
            case IMAGE_CONTENT -> builder.content(msg.getImageContent().getImageUrl());
            case TEXT_AND_IMAGE_CONTENT -> builder.content(msg.getTextAndImageContent().getTextContent().getText());
            case FILE_CONTENT -> builder.content(msg.getFileContent().getFileUrl());
            case VIDEO_CONTENT -> builder.content(msg.getVideoContent().getVideoUrl());
            case AUDIO_CONTENT -> builder.content(msg.getAudioContent().getAudioUrl());
            case RECALL_CONTENT -> builder.content(msg.getRecallContent().getRecallMessageId());
            case FRIEND_REQUEST_EVENT -> builder.content(msg.getFriendRequestEvent().getRequestId() + ":" + msg.getFriendRequestEvent().getStatus().name());
            case PONG_RESPONSE, PONG_PING, AUTH_MESSAGE, PRESENCE_MESSAGE -> builder.content(msg.getContentCase().name());
            default -> builder.content("unknown");
        }

        return builder.build();
    }
}
