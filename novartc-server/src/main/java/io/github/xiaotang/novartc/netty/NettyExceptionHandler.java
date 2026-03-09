package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.config.ChatErrorLog;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.TimeoutException;

import java.io.IOException;

/**
 * Created on 2026/3/7 11:20
 *
 * @author tangj
 * @description
 */
public class NettyExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            ChatErrorLog.log.debug("客户端断开 {}", ctx.channel().remoteAddress());
        } else if (cause instanceof TimeoutException) {
            ChatErrorLog.log.warn("连接超时 {}", ctx.channel().remoteAddress());
        } else {
            ChatErrorLog.log.error("Netty异常", cause);
        }
        UserChannelManager.remove(ctx.channel().attr(NettyAttributes.USER_ID).get(), ctx.channel());
        ctx.close();
    }
}
