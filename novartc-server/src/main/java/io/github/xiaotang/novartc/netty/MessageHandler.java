package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.config.ChatMsgLog;
import io.github.xiaotang.novartc.proto.ChatMessage;
import io.github.xiaotang.novartc.util.JacksonUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created on 2026/3/5 14:07
 *
 * @author tangj
 * @description
 */
public class MessageHandler extends SimpleChannelInboundHandler<ChatMessage.MsgBody> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                ChatMessage.MsgBody msgBody) {

        Channel channel = ctx.channel();

        String userId = channel.attr(NettyAttributes.USER_ID).get();

        if (userId == null) {
            ChatMsgLog.log.warn("unauthorized message: {}", JacksonUtil.toJson(msgBody));
            channel.close();
            return;
        }
        ctx.channel()
                .attr(NettyAttributes.LAST_HEARTBEAT)
                .set(System.currentTimeMillis());

        ChatMsgLog.msg(msgBody);

        switch (msgBody.getMessageType()) {

            case TEXT -> handleTextMessage(userId, msgBody);

            case IMAGE -> handleImageMessage(userId, msgBody);

            case PING -> ChatMsgLog.log.info("receive ping {}", userId);

            case PONG -> ChatMsgLog.log.info("receive pong {}", userId);

            default -> ChatMsgLog.log.warn("unsupported type {}, from {}", msgBody.getMessageType(), userId);
        }
    }

    private void handleTextMessage(String userId, ChatMessage.MsgBody msgBody) {

    }

    private void handleImageMessage(String userId, ChatMessage.MsgBody msgBody) {

    }
}
