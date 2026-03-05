package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.proto.ChatMessage;

/**
 * Created on 2026/3/5 14:45
 *
 * @author tangj
 * @description
 */
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

        // 单聊
        if (!msg.getIsGroup() && msg.hasSingleChatInfo()) {
            builder.chatId(msg.getSingleChatInfo().getChatId());
            builder.targetId(msg.getSingleChatInfo().getUser2Id());
        }

        // 群聊
        if (msg.getIsGroup() && msg.hasGroupChatInfo()) {
            builder.chatId(msg.getGroupChatInfo().getChatId());
            builder.targetId(msg.getGroupChatInfo().getGroupId());
        }

        // 内容
        switch (msg.getContentCase()) {

            case TEXT_CONTENT:
                builder.content(msg.getTextContent().getText());
                break;

            case IMAGE_CONTENT:
                builder.content(msg.getImageContent().getImageUrl());
                break;

            case TEXT_AND_IMAGE_CONTENT:
                builder.content(
                        msg.getTextAndImageContent().getTextContent().getText()
                );
                break;

            case PONG_RESPONSE:
            case PONG_PING:
            case AUTH_MESSAGE:
                builder.content(msg.getContentCase().name());
                break;

            default:
                builder.content("unknown");
        }

        return builder.build();
    }
}
