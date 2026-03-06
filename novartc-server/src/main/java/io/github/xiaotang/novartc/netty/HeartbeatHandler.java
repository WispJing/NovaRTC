package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.config.ChatAccessLog;
import io.github.xiaotang.novartc.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Created on 2026/3/5 14:57
 *
 * @author tangj
 * @description
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof ChatMessage.MsgBody body) {

            if (body.getMessageType() == ChatMessage.MessageType.PING) {
                handlePing(ctx, body);
                return;
            }

            if (body.getMessageType() == ChatMessage.MessageType.PONG) {
                resetHeartbeat(ctx.channel());
                return;
            }
        }

        super.channelRead(ctx, msg);
    }

    private void handlePing(ChannelHandlerContext ctx, ChatMessage.MsgBody ping) {
        Channel channel = ctx.channel();
        resetHeartbeat(channel);
        ChatMessage.MsgBody pong =
                ChatMessage.MsgBody.newBuilder()
                        .setMessageType(ChatMessage.MessageType.PONG)
                        .setPongResponse(
                                ChatMessage.Pong.newBuilder()
                                        .setPingId(ping.getPongPing().getPingId())
                                        .build()
                        )
                        .build();
        channel.writeAndFlush(pong);
    }

    private void resetHeartbeat(Channel channel) {
        channel.attr(NettyAttributes.HEARTBEAT_COUNT).set(0);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {

        if (evt instanceof IdleStateEvent event) {

            if (event.state() == IdleState.READER_IDLE) {
                Channel channel = ctx.channel();
                Integer count = channel.attr(NettyAttributes.HEARTBEAT_COUNT).get();

                if (count == null) {
                    count = 0;
                }
                count++;

                channel.attr(NettyAttributes.HEARTBEAT_COUNT).set(count);

                if (count >= 2) {
                    ChatAccessLog.log.warn("心跳超时关闭连接 {}", channel.id());
                    String userId = channel.attr(NettyAttributes.USER_ID).get();
                    UserChannelManager.remove(userId, channel);
                    channel.close();
                } else {
                    ChatAccessLog.log.warn("检测到读空闲，发送Ping {}", channel.id());
                    sendPing(channel);
                }
            }
        }
    }

    private void sendPing(Channel channel) {

        ChatMessage.MsgBody ping =
                ChatMessage.MsgBody.newBuilder()
                        .setMessageType(ChatMessage.MessageType.PING)
                        .setPongPing(
                                ChatMessage.Ping.newBuilder()
                                        .setPingId(UUID.randomUUID().toString())
                                        .build()
                        )
                        .build();

        channel.writeAndFlush(ping);
    }
}
