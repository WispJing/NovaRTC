package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatErrorLog;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created on 2026/3/7 11:20
 *
 * @author tangj
 * @description
 */
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class NettyExceptionHandler extends ChannelInboundHandlerAdapter {
    private final UserChannelManager userChannelManager;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            ChatErrorLog.log.debug("客户端连接已断开: remoteAddress={}", ctx.channel().remoteAddress());
        } else if (cause instanceof TimeoutException) {
            ChatErrorLog.log.warn("连接读写超时: remoteAddress={}", ctx.channel().remoteAddress());
        } else {
            ChatErrorLog.log.error("Netty 通信异常", cause);
        }
        userChannelManager.remove(ctx.channel());
        ctx.close();
    }
}
