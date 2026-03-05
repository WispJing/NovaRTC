package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2026/3/5 14:07
 *
 * @author tangj
 * @description
 */
public class MessageHandler extends SimpleChannelInboundHandler<ChatMessage.MsgBody> {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                ChatMessage.MsgBody msgBody) {

        Channel channel = ctx.channel();

        String userId = channel.attr(NettyAttributes.USER_ID).get();

        if (userId == null) {
            logger.warn("unauthorized message");
            channel.close();
            return;
        }

        switch (msgBody.getMessageType()) {

            case TEXT -> handleTextMessage(userId, msgBody);

            case IMAGE -> handleImageMessage(userId, msgBody);

            case PING -> logger.info("receive ping {}", userId);

            case PONG -> logger.info("receive pong {}", userId);

            default -> logger.warn("unsupported type {}", msgBody.getMessageType());
        }
    }

    private void handleTextMessage(String userId, ChatMessage.MsgBody msgBody) {

        logger.info("TEXT from {} : {}",
                userId,
                msgBody.getTextContent().getText());
    }

    private void handleImageMessage(String userId, ChatMessage.MsgBody msgBody) {

        logger.info("IMAGE from {} : {}",
                userId,
                msgBody.getImageContent().getImageUrl());
    }
}
