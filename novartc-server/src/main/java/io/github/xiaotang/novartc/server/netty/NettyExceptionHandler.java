package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatErrorLog;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
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
public class NettyExceptionHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private UserChannelManager userChannelManager;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            ChatErrorLog.log.debug("客户端断开 {}", ctx.channel().remoteAddress());
        } else if (cause instanceof TimeoutException) {
            ChatErrorLog.log.warn("连接超时 {}", ctx.channel().remoteAddress());
        } else {
            ChatErrorLog.log.error("Netty异常", cause);
        }
        userChannelManager.remove(ctx.channel());
        ctx.close();
    }
}
