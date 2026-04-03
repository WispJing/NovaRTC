package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class WebSocketMsgHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMsgHandler.class);

    public static void sendSystemMessage(ChannelHandlerContext ctx, String message) {
        sendMessage(ctx, ChatMessage.MessageType.SYSTEM, message, null, null, null, null, null, null);
    }

    public static void sendAuthState(ChannelHandlerContext ctx, boolean isSuccess) {
        sendMessage(ctx, ChatMessage.MessageType.SYSTEM, isSuccess ? "认证成功" : "认证失败",
                null, null, null, null, null, null);
    }

    public static void sendTextMessage(ChannelHandlerContext ctx, String messageId, String text, String description,
                                       ImUser sendUser, ImGroupChat imGroupChat, ImSingleChat imSingleChat) {
        sendMessage(ctx, ChatMessage.MessageType.TEXT, text, messageId, sendUser, imGroupChat, imSingleChat, null, null);
    }

    public static void sendImageMessage(ChannelHandlerContext ctx, String messageId, String imageUrl, String description,
                                        ImUser sendUser, ImGroupChat imGroupChat, ImSingleChat imSingleChat) {
        sendMessage(ctx, ChatMessage.MessageType.IMAGE, description, messageId, sendUser, imGroupChat, imSingleChat, imageUrl, description);
    }

    public static void sendTextAndImageMessage(ChannelHandlerContext ctx, String messageId, String text, String imageUrl,
                                               String description, ImUser sendUser, ImGroupChat imGroupChat, ImSingleChat imSingleChat) {
        sendMessage(ctx, ChatMessage.MessageType.TEXT_AND_IMAGE, text, messageId, sendUser, imGroupChat, imSingleChat, imageUrl, description);
    }

    public static void sendMessage(ChannelHandlerContext ctx, ChatMessage.MessageType messageType, String textContent,
                                   String messageId, ImUser sendUser, ImGroupChat imGroupChat, ImSingleChat imSingleChat,
                                   String imageUrl, String description) {
        try {
            ChatMessage.MsgBody.Builder msgBodyBuilder = ChatMessage.MsgBody.newBuilder()
                    .setMessageType(messageType)
                    .setTimestamp(System.currentTimeMillis());

            if (StringUtils.isNotBlank(messageId)) {
                msgBodyBuilder.setMessageId(messageId);
            }

            if (Objects.nonNull(sendUser)) {
                msgBodyBuilder.setSender(buildUser(sendUser));
            }

            if (Objects.nonNull(imGroupChat)) {
                msgBodyBuilder.setIsGroup(true)
                        .setGroupChatInfo(ChatMessage.GroupChatInfo.newBuilder()
                                .setChatId(imGroupChat.getChatId())
                                .setGroupId(imGroupChat.getId()));
            }

            if (Objects.nonNull(imSingleChat)) {
                msgBodyBuilder.setSingleChatInfo(ChatMessage.SingleChatInfo.newBuilder()
                        .setChatId(imSingleChat.getChatId())
                        .setUser1Id(imSingleChat.getUser1Id())
                        .setUser2Id(imSingleChat.getUser2Id()));
            }

            if (messageType == ChatMessage.MessageType.TEXT) {
                msgBodyBuilder.setTextContent(ChatMessage.TextContent.newBuilder().setText(textContent).build());
            } else if (messageType == ChatMessage.MessageType.IMAGE) {
                msgBodyBuilder.setImageContent(ChatMessage.ImageContent.newBuilder()
                        .setImageUrl(imageUrl)
                        .setImageDescription(description).build());
            } else if (messageType == ChatMessage.MessageType.TEXT_AND_IMAGE) {
                msgBodyBuilder.setTextAndImageContent(ChatMessage.TextAndImageContent.newBuilder()
                        .setTextContent(ChatMessage.TextContent.newBuilder().setText(textContent).build())
                        .setImageContent(ChatMessage.ImageContent.newBuilder().setImageUrl(imageUrl).setImageDescription(description).build())
                        .build());
            }

            byte[] byteArray = msgBodyBuilder.build().toByteArray();
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(byteArray)));
        } catch (Exception e) {
            logger.error("WebSocket 消息发送失败", e);
        }
    }

    public static ChatMessage.User buildUser(ImUser imUser) {
        return ChatMessage.User.newBuilder()
                .setUserId(imUser.getId())
                .setUsername(imUser.getUsername())
                .setAvatarUrl(imUser.getAvatarUrl())
                .build();
    }
}
