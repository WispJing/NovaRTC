package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.config.ChatAppLog;
import io.github.xiaotang.novartc.config.ChatErrorLog;
import io.github.xiaotang.novartc.constant.IMConstant;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;

/**
 * Created on 2026/2/28 9:05
 *
 * @author tangj
 * @description
 */
public class NovaRTCServer extends BaseServer {

    @Override
    public void start() {
        b.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 4096)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                new ProtocolDetectHandler(),
                                new IdleStateHandler(60, 0, 0),
                                new NettyExceptionHandler()
                        );
                    }
                });
        try {
            cf = b.bind(IMConstant.port).sync();
            InetSocketAddress addr = (InetSocketAddress) cf.channel().localAddress();
            ChatAppLog.log.info("NovaRTC IM server is running, port is: {}", addr.getPort());
        } catch (InterruptedException e) {
            ChatErrorLog.log.error("NovaRTC IM server start fail,", e);
        }
    }
}
