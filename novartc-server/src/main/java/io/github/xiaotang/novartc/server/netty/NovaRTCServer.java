package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAppLog;
import io.github.xiaotang.novartc.server.config.ChatErrorLog;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * Created on 2026/2/28 9:05
 *
 * @author tangj
 * @description
 */
@Component
@RequiredArgsConstructor
public class NovaRTCServer extends BaseServer {
    private final IMConfigProperties imConfigProperties;
    private final ProtocolDetectHandler protocolDetectHandler;
    private final NettyExceptionHandler nettyExceptionHandler;

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
                // 写缓冲水位线：超过高水位后 isWritable 返回 false，配合背压检查保护服务端内存
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(320 * 1024, 640 * 1024))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(
                                protocolDetectHandler,
                                new IdleStateHandler(60, 0, 0),
                                nettyExceptionHandler
                        );
                    }
                });
        try {
            cf = b.bind(imConfigProperties.getPort()).sync();
            InetSocketAddress addr = (InetSocketAddress) cf.channel().localAddress();
            ChatAppLog.log.info("NovaRTC IM 服务启动完成: port={}", addr.getPort());
        } catch (InterruptedException e) {
            ChatErrorLog.log.error("NovaRTC IM 服务启动失败", e);
        }
    }
}
