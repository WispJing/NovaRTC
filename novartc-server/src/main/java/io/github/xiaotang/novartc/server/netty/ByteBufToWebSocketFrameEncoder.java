package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * Created on 2026/3/5 11:08
 *
 * @author tangj
 * @description
 */
public class ByteBufToWebSocketFrameEncoder extends MessageToMessageEncoder<ChatMessage.MsgBody> {
    @Override
    protected void encode(ChannelHandlerContext ctx,
                          ChatMessage.MsgBody msg,
                          List<Object> out) {

        byte[] bytes = msg.toByteArray();

        ByteBuf buf = ctx.alloc().buffer(bytes.length);
        buf.writeBytes(bytes);

        out.add(new BinaryWebSocketFrame(buf));
    }
}
