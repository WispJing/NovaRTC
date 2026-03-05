package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2026/2/28 14:32
 *
 * @author tangj
 * @description
 */
public class UserAuthHandler extends SimpleChannelInboundHandler<ChatMessage.MsgBody> {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                ChatMessage.MsgBody msgBody) {

        Channel channel = ctx.channel();

        switch (msgBody.getMessageType()) {

            case TEXT -> handleTextMessage(channel, msgBody);

            case IMAGE -> handleImageMessage(channel, msgBody);

            case PING -> logger.info("receive ping");

            case PONG -> logger.info("receive pong");

            case AUTH -> handleAuthMessage(channel, msgBody);

            default -> logger.warn("unsupported type {}", msgBody.getMessageType());
        }
    }

    private void handleTextMessage(Channel channel, ChatMessage.MsgBody msgBody) {

        logger.info("TEXT: {}", msgBody.getTextContent().getText());
    }

    private void handleImageMessage(Channel channel, ChatMessage.MsgBody msgBody) {

        logger.info("IMAGE: {}", msgBody.getImageContent().getImageUrl());
    }

    private void handleAuthMessage(Channel channel, ChatMessage.MsgBody msgBody) {

        String userId = msgBody.getSender().getUserId();
        String username = msgBody.getSender().getUsername();

        logger.info("auth user {} {}", userId, username);
    }

}
