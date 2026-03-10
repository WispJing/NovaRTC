package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.ChatMsgLog;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on 2026/3/5 14:07
 *
 * @author tangj
 * @description
 */
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
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

            case TEXT, IMAGE, VIDEO, AUDIO, FILE, TEXT_AND_IMAGE -> handleMessage(userId, msgBody);

            default -> ChatMsgLog.log.warn("unsupported type {}, from {}", msgBody.getMessageType(), userId);
        }
    }

    private void handleMessage(String userId, ChatMessage.MsgBody msgBody) {
        ChatMsgLog.msg(msgBody);

    }
}
