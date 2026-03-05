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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                //由于使用自定义的心跳机制，所以没必要加上 childOption(ChannelOption.SO_KEEPALIVE, true);
                //而且SO_KEEPALIVE默认是每3600s发送一次心跳，不能及时检测到链路的有效性
                .localAddress(new InetSocketAddress(IMConstant.port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new ProtocolDetectHandler());
                    }
                });

        try {
            cf = b.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) cf.channel().localAddress();
            ChatAppLog.log.info("NovaRTC IM server is running, port is: {}", addr.getPort());
        } catch (InterruptedException e) {
            ChatErrorLog.log.error("NovaRTC IM server start fail,", e);
        }
    }
}
