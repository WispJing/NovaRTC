package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
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
    private boolean isHttp(ByteBuf buf) {

        if (buf.readableBytes() < 4) {
            return false;
        }

        char c0 = (char) buf.getByte(0);
        char c1 = (char) buf.getByte(1);
        char c2 = (char) buf.getByte(2);
        char c3 = (char) buf.getByte(3);

        return (c0 == 'G' && c1 == 'E' && c2 == 'T') ||
                (c0 == 'P' && c1 == 'O' && c2 == 'S' && c3 == 'T') ||
                (c0 == 'P' && c1 == 'U' && c2 == 'T') ||
                (c0 == 'D' && c1 == 'E' && c2 == 'L') ||
                (c0 == 'O' && c1 == 'P' && c2 == 'T');
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
