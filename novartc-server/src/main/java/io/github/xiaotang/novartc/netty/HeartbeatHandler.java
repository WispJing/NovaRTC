package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Created on 2026/3/5 14:57
 *
 * @author tangj
 * @description
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log =
            LoggerFactory.getLogger(HeartbeatHandler.class);

    private static final AttributeKey<Integer> HEARTBEAT_COUNT = AttributeKey.valueOf("heartbeatCount");

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {

        if (evt instanceof IdleStateEvent event) {

            if (event.state() == IdleState.READER_IDLE) {

                Channel channel = ctx.channel();

                Integer count =
                        channel.attr(HEARTBEAT_COUNT).get();

                if (count == null) {
                    count = 0;
                }

                count++;

                channel.attr(HEARTBEAT_COUNT).set(count);

                if (count >= 2) {

                    log.warn("心跳超时关闭连接 {}", channel.id());

                    UserChannelManager.remove(channel.attr(NettyAttributes.USER_ID).get());

                    channel.close();

                } else {

                    log.warn("检测到读空闲，发送Ping {}", channel.id());

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
