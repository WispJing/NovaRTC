package io.github.xiaotang.novartc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

/**
 * Created on 2026/3/5 11:07
 *
 * @author tangj
 * @description
 */
public class WebSocketFrameToByteBufDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {
    @Override
    protected void decode(ChannelHandlerContext ctx,
                          BinaryWebSocketFrame frame,
                          List<Object> out) {

        out.add(frame.content().retain());
    }
}
