package io.github.xiaotang.novartc.netty;

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
public class ByteBufToWebSocketFrameEncoder extends MessageToMessageEncoder<ByteBuf> {
    @Override
    protected void encode(ChannelHandlerContext ctx,
                          ByteBuf msg,
                          List<Object> out) {

        out.add(new BinaryWebSocketFrame(msg.retain()));
    }
}
