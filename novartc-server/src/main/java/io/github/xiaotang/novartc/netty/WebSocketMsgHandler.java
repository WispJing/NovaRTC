package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.entity.mongodb.IMGroupChat;
import io.github.xiaotang.novartc.entity.mongodb.IMSingleChat;
import io.github.xiaotang.novartc.entity.mongodb.IMUser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Created on 2026/2/28 15:53
 *
 * @author tangj
 * @description
 */
public class WebSocketMsgHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMsgHandler.class);

    /**
     * 发送系统消息（比如 msg type not supported）
     *
     * @param ctx     Netty ChannelHandlerContext
     * @param message 系统消息内容
     */
    public static void sendSystemMessage(ChannelHandlerContext ctx, String message) {
        sendMessage(ctx, ChatMessage.MessageType.SYSTEM, message, null, null, null, null, null, null);
    }

    /**
     * 发送认证消息的工具方法
     *
     * @param ctx       Netty ChannelHandlerContext
     * @param isSuccess 认证结果
     */
    public static void sendAuthState(ChannelHandlerContext ctx, boolean isSuccess) {
        sendMessage(ctx, ChatMessage.MessageType.SYSTEM, "Authentication " + (isSuccess ? "successful" : "failed"),
                null, null, null, null, null, null);
    }

    /**
     * 发送文本消息
     *
     * @param ctx Netty ChannelHandlerContext
     */
    public static void sendTextMessage(ChannelHandlerContext ctx, String messageId, String text, String description,
                                       IMUser sendUser, IMGroupChat imGroupChat, IMSingleChat imSingleChat) {
        sendMessage(ctx, ChatMessage.MessageType.TEXT, text, messageId, sendUser, imGroupChat, imSingleChat, null, null);
    }

    /**
     * 发送图片消息
     *
     * @param ctx Netty ChannelHandlerContext
     */
    public static void sendImageMessage(ChannelHandlerContext ctx, String messageId, String imageUrl, String description,
                                        IMUser sendUser, IMGroupChat imGroupChat, IMSingleChat imSingleChat) {
        sendMessage(ctx, ChatMessage.MessageType.IMAGE, description, messageId, sendUser, imGroupChat, imSingleChat, imageUrl, description);
    }

    /**
     * 发送复合消息（文本+图片）
     *
     * @param ctx Netty ChannelHandlerContext
     */
    public static void sendTextAndImageMessage(ChannelHandlerContext ctx, String messageId, String text, String imageUrl,
                                               String description, IMUser sendUser, IMGroupChat imGroupChat, IMSingleChat imSingleChat) {
        sendMessage(ctx, ChatMessage.MessageType.TEXT_AND_IMAGE, text, messageId, sendUser, imGroupChat, imSingleChat, imageUrl, description);
    }

    /**
     * 通用的发送消息方法
     */
    public static void sendMessage(ChannelHandlerContext ctx, ChatMessage.MessageType messageType, String textContent,
                                   String messageId, IMUser sendUser, IMGroupChat imGroupChat, IMSingleChat imSingleChat,
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

            // 设置群聊信息
            if (Objects.nonNull(imGroupChat)) {
                msgBodyBuilder.setIsGroup(true)
                        .setGroupChatInfo(ChatMessage.GroupChatInfo.newBuilder()
                                .setChatId(imGroupChat.getChatId())
                                .setGroupId(imGroupChat.getId()));
            }

            // 设置单聊信息
            if (Objects.nonNull(imSingleChat)) {
                msgBodyBuilder.setSingleChatInfo(ChatMessage.SingleChatInfo.newBuilder()
                        .setChatId(imSingleChat.getChatId())
                        .setUser1Id(imSingleChat.getUser1Id())
                        .setUser2Id(imSingleChat.getUser2Id()));
            }

            // 设置消息内容
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
            logger.error("Failed to send message", e);
        }
    }

    public static ChatMessage.User buildUser(IMUser imUser) {
        return ChatMessage.User.newBuilder()
                .setUserId(imUser.getUserNum())
                .setUsername(imUser.getUsername())
                .setAvatarUrl(imUser.getAvatarUrl())
                .build();
    }
}
