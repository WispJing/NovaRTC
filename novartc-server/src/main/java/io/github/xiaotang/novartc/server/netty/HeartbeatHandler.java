package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created on 2026/3/5 14:57
 *
 * @author tangj
 * @description
 */
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    private final UserChannelManager userChannelManager;
    private final UserRouteService userRouteService;
    private final InstanceIdProvider instanceIdProvider;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof ChatMessage.MsgBody body) {
            touchRoute(ctx.channel());

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

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        userChannelManager.remove(ctx.channel());
        super.channelInactive(ctx);
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
        touchRoute(channel);
    }

    private void touchRoute(Channel channel) {
        String userId = channel.attr(NettyAttributes.USER_ID).get();
        if (StringUtils.isBlank(userId)) {
            return;
        }
        userRouteService.renewUser(userId, instanceIdProvider.getInstanceId());
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
                    ChatAccessLog.log.warn("心跳检测超时，连接即将关闭: channelId={}", channel.id());
                    channel.close();
                } else {
                    ChatAccessLog.log.info("检测到读空闲，已发送心跳探测: channelId={}", channel.id());
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
