package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.config.ChatAccessLog;
import io.github.xiaotang.novartc.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2026/3/5 15:12
 *
 * @author tangj
 * @description
 */
public class HeartbeatCheckTask implements TimerTask {
    private final Channel channel;

    public HeartbeatCheckTask(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void run(Timeout timeout) {

        if (!channel.isActive()) {
            return;
        }

        Long lastHeartbeat = channel.attr(NettyAttributes.LAST_HEARTBEAT).get();

        long now = System.currentTimeMillis();

        if (now - lastHeartbeat > 60000) {

            Integer count = channel.attr(NettyAttributes.HEARTBEAT_COUNT).get();

            if (count == null) {
                count = 0;
            }

            count++;

            channel.attr(NettyAttributes.HEARTBEAT_COUNT).set(count);

            if (count >= 2) {

                ChatAccessLog.log.warn("心跳超时关闭连接 {}", channel.id());

                UserChannelManager.remove(channel.attr(NettyAttributes.USER_ID).get());

                channel.close();

                return;
            } else {

                ChatAccessLog.log.warn("检测到读空闲，发送Ping {}", channel.id());

                sendPing(ctx);
            }
        }

        // 重新注册检测任务
        HeartbeatTimer.TIMER.newTimeout(
                new HeartbeatCheckTask(channel),
                60,
                TimeUnit.SECONDS
        );
    }

    private void sendPing(ChannelHandlerContext ctx) {

        ChatMessage.MsgBody ping =
                ChatMessage.MsgBody.newBuilder()
                        .setMessageType(ChatMessage.MessageType.PING)
                        .setPongPing(
                                ChatMessage.Ping.newBuilder()
                                        .setPingId(UUID.randomUUID().toString())
                                        .build()
                        )
                        .build();

        ctx.writeAndFlush(ping);
    }
}
