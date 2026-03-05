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

        switch (msgBody.getMessageType()) {
            case PING -> ChatMsgLog.log.info("receive ping {}", userId);

            case PONG -> ChatMsgLog.log.info("receive pong {}", userId);

            case TEXT, IMAGE, VIDEO, AUDIO, FILE, TEXT_AND_IMAGE  -> handleMessage(userId, msgBody);

            default -> ChatMsgLog.log.warn("unsupported type {}, from {}", msgBody.getMessageType(), userId);
        }
    }

    private void handleMessage(String userId, ChatMessage.MsgBody msgBody) {
        ChatMsgLog.msg(msgBody);

    }
}
