package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.entity.mongodb.IMGroupChat;
import io.github.xiaotang.novartc.entity.mongodb.IMSingleChat;
import io.github.xiaotang.novartc.entity.mongodb.IMUser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Created on 2026/2/28 15:53
 *
 * @author tangj
 * @description
 */
@Component
public class WebSocketMsgHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketMsgHandler.class);

    /**
     * 发送系统消息（比如 msg type not supported）
     *
     * @param ctx     Netty ChannelHandlerContext
     * @param message 系统消息内容
     */
    private void sendSystemMessage(ChannelHandlerContext ctx, String message) {
        try {
            ChatMessage.MsgBody msgBody = ChatMessage.MsgBody.newBuilder()
                    .setMessageType(ChatMessage.MessageType.SYSTEM)
                    .setTextContent(ChatMessage.TextContent.newBuilder().setText(message).build())
                    .build();

            byte[] byteArray = msgBody.toByteArray();
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(byteArray)));
        } catch (Exception e) {
            logger.error("Failed to send system message", e);
        }
    }

    /**
     * 发送认证消息的工具方法
     *
     * @param ctx       Netty ChannelHandlerContext
     * @param isSuccess 认证结果
     */
    private void sendAuthState(ChannelHandlerContext ctx, boolean isSuccess) {
        try {
            ChatMessage.MsgBody msgBody = ChatMessage.MsgBody.newBuilder()
                    .setMessageType(ChatMessage.MessageType.SYSTEM)
                    .setTextContent(ChatMessage.TextContent.newBuilder().setText("Authentication " + (isSuccess ? "successful" : "failed")).build())
                    .build();

            byte[] byteArray = msgBody.toByteArray();
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(byteArray)));
        } catch (Exception e) {
            logger.error("Failed to send authentication state message", e);
        }
    }

    /**
     * 发送文本消息
     *
     * @param ctx  Netty ChannelHandlerContext
     */
    private void sendTextMessage(ChannelHandlerContext ctx
            , String messageId, String text, String description
            , IMUser sendUser, IMGroupChat imGroupChat, IMSingleChat imSingleChat) {
        try {
            ChatMessage.MsgBody.Builder msgBodyBuilder = ChatMessage.MsgBody.newBuilder()
                    .setMessageId(messageId)
                    .setMessageType(ChatMessage.MessageType.TEXT)
                    .setTimestamp(System.currentTimeMillis())
                    .setSender(buildUser(sendUser))
                    .setTextContent(ChatMessage.TextContent.newBuilder()
                            .setText(text)
                            .build());
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

            byte[] byteArray = msgBodyBuilder.build().toByteArray();
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(byteArray)));
        } catch (Exception e) {
            logger.error("Failed to send text message", e);
        }
    }

    /**
     * 发送图片消息
     *
     * @param ctx         Netty ChannelHandlerContext
     */
    private void sendImageMessage(ChannelHandlerContext ctx
            , String messageId, String imageUrl, String description
            , IMUser sendUser, IMGroupChat imGroupChat, IMSingleChat imSingleChat) {
        try {
            ChatMessage.MsgBody.Builder msgBodyBuilder = ChatMessage.MsgBody.newBuilder()
                    .setMessageId(messageId)
                    .setMessageType(ChatMessage.MessageType.IMAGE)
                    .setTimestamp(System.currentTimeMillis())
                    .setSender(buildUser(sendUser))
                    .setImageContent(ChatMessage.ImageContent.newBuilder()
                            .setImageUrl(imageUrl)
                            .setImageDescription(description).build());
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

            byte[] byteArray = msgBodyBuilder.build().toByteArray();
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(byteArray)));
        } catch (Exception e) {
            logger.error("Failed to send image message", e);
        }
    }

    /**
     * 发送复合消息（文本+图片）
     *
     * @param ctx         Netty ChannelHandlerContext
     */
    private void sendTextAndImageMessage(ChannelHandlerContext ctx
            , String messageId, String text, String imageUrl, String description
            , IMUser sendUser, IMGroupChat imGroupChat, IMSingleChat imSingleChat) {
        try {
            ChatMessage.MsgBody.Builder msgBodyBuilder = ChatMessage.MsgBody.newBuilder()
                    .setMessageId(messageId)
                    .setMessageType(ChatMessage.MessageType.TEXT_AND_IMAGE)
                    .setTimestamp(System.currentTimeMillis())
                    .setSender(buildUser(sendUser))
                    .setTextAndImageContent(ChatMessage.TextAndImageContent.newBuilder()
                            .setTextContent(ChatMessage.TextContent.newBuilder().setText(text).build())
                            .setImageContent(ChatMessage.ImageContent.newBuilder().setImageUrl(imageUrl).setImageDescription(description).build())
                            .build());
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

            byte[] byteArray = msgBodyBuilder.build().toByteArray();
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(byteArray)));
        } catch (Exception e) {
            logger.error("Failed to send text and image message", e);
        }
    }

    private ChatMessage.User buildUser(IMUser imUser) {
        return ChatMessage.User.newBuilder()
                .setUserId(imUser.getUserNum())
                .setUsername(imUser.getUsername())
                .setAvatarUrl(imUser.getAvatarUrl())
                .build();
    }
}
