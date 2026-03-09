package io.github.xiaotang.novartc.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Created on 2026/3/5 11:06
 *
 * @author tangj
 * @description 去掉Magic
 */
public class TcpMagicDecoder extends ByteToMessageDecoder {
    private static final short MAGIC = (short) 0xCAFE;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {

        if (in.readableBytes() < 2) {
            return;
        }

        short magic = in.readShort();

        if (magic != MAGIC) {
            ctx.close();
            return;
        }

        out.add(in.readBytes(in.readableBytes()));
    }
}
