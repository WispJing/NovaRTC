package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.proto.ChatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

/**
 * Created on 2026/3/5 11:03
 *
 * @author tangj
 * @description
 */
public class ProtocolDetectHandler extends ChannelInboundHandlerAdapter {
    private static final short MAGIC = (short) 0xCAFE;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf in)) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (in.readableBytes() < 4) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (isHttp(in)) {
            switchToWebSocket(ctx);
            ctx.fireChannelRead(msg);
        } else if (isTcpMagic(in)) {
            switchToTcp(ctx);
            ctx.fireChannelRead(msg);
        } else {
            in.release();
            ctx.close();
        }
    }

    /**
     * 检测HTTP协议
     */
    // 优化后的HTTP检测
    private boolean isHttp(ByteBuf buf) {
        if (buf.readableBytes() < 4) {
            return false;
        }
        // 保存当前读取位置
        int readerIndex = buf.readerIndex();
        try {
            // 读取前几个字节，判断是否为HTTP方法
            byte[] bytes = new byte[Math.min(buf.readableBytes(), 10)];
            buf.readBytes(bytes);
            String method = new String(bytes).trim();
            return method.equals(HttpMethod.GET.name()) ||
                    method.equals(HttpMethod.POST.name()) ||
                    method.equals(HttpMethod.PUT.name()) ||
                    method.equals(HttpMethod.DELETE.name()) ||
                    method.equals(HttpMethod.OPTIONS.name());
        } finally {
            // 恢复读取位置
            buf.readerIndex(readerIndex);
        }
    }

    /**
     * 检测TCP Magic
     */
    private boolean isTcpMagic(ByteBuf buf) {

        if (buf.readableBytes() < 2) {
            return false;
        }

        short magic = buf.getShort(0);
        return magic == MAGIC;
    }

    /**
     * 切换TCP pipeline
     */
    private void switchToTcp(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();

        p.remove(this);

        p.addLast(new LengthFieldBasedFrameDecoder(
                10 * 1024 * 1024,
                2,
                4,
                0,
                6));

        p.addLast(new TcpMagicDecoder());

        p.addLast(new LengthFieldPrepender(4));
        p.addLast(new ProtobufDecoder(ChatMessage.MsgBody.getDefaultInstance()));
        p.addLast(new ProtobufEncoder());

        p.addLast(new HeartbeatHandler());
        p.addLast(new UserAuthHandler());
        p.addLast(new MessageHandler());
    }

    /**
     * 切换WebSocket pipeline
     */
    private void switchToWebSocket(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();

        p.remove(this);

        p.addLast(new HttpServerCodec());
        p.addLast(new HttpObjectAggregator(65536));

        p.addLast(new WebSocketServerProtocolHandler("/ws", null, true));

        p.addLast(new WebSocketFrameToByteBufDecoder());

        p.addLast(new ProtobufDecoder(ChatMessage.MsgBody.getDefaultInstance()));
        p.addLast(new ProtobufEncoder());

        p.addLast(new ByteBufToWebSocketFrameEncoder());

        p.addLast(new HeartbeatHandler());
        p.addLast(new UserAuthHandler());
        p.addLast(new MessageHandler());
    }
}
