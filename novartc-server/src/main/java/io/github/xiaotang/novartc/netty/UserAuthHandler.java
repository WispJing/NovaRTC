package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.proto.ChatMessage;
import io.github.xiaotang.novartc.util.NettyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
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

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            // 判断Channel是否读空闲, 读空闲时移除Channel
            if (event.state().equals(IdleState.READER_IDLE)) {
                final String remoteAddress = NettyUtil.parseChannelRemoteAddr(ctx.channel());
                logger.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
//                UserInfoManager.removeChannel(ctx.channel());
//                UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT,UserInfoManager.getAuthUserCount());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

}
