package io.github.xiaotang.novartc.netty;

import com.alibaba.fastjson.JSONObject;
import io.github.xiaotang.novartc.constant.IMConstant;
import io.github.xiaotang.novartc.util.NettyUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Created on 2026/2/28 14:32
 *
 * @author tangj
 * @description
 */
public class UserAuthHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthHandler.class);
    private WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocket(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            // 判断Channel是否读空闲, 读空闲时移除Channel
            if (event.state().equals(IdleState.READER_IDLE)) {
                final String remoteAddress = NettyUtil.parseChannelRemoteAddr(ctx.channel());
                logger.warn("NETTY SERVER PIPELINE: IDLE exception [{}]", remoteAddress);
                UserInfoManager.removeChannel(ctx.channel());
                UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess() || !"websocket".equals(request.headers().get("Upgrade"))) {
            logger.warn("protobuf don't support websocket");
            ctx.channel().close();
            return;
        }
        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
                IMConstant.websocketUrl, null, true);
        handshaker = handshakerFactory.newHandshaker(request);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            // 动态加入websocket的编解码处理
            handshaker.handshake(ctx.channel(), request);
            // 存储已经连接的Channel
            UserInfoManager.addChannel(ctx.channel());
        }
    }

    private void handleWebSocket(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路命令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            UserInfoManager.removeChannel(ctx.channel());
            return;
        }
        // 判断是否Ping消息
        if (frame instanceof PingWebSocketFrame) {
            logger.info("ping message:{}", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 判断是否Pong消息
        if (frame instanceof PongWebSocketFrame) {
            logger.info("pong message:{}", frame.content().retain());
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        // 本程序目前只支持Protobuf消息
        if (!(frame instanceof BinaryWebSocketFrame)) {
            throw new UnsupportedOperationException(frame.getClass().getName() + " frame type not supported");
        }
        // 解析Protobuf消息
        BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) frame;
        byte[] byteArray = new byte[binaryFrame.content().readableBytes()];
        binaryFrame.content().readBytes(byteArray);

        ChatMessage.MsgBody msgBody;
        try {
            msgBody = ChatMessage.MsgBody.parseFrom(byteArray);
        } catch (Exception e) {
            logger.error("Failed to parse protobuf message", e);
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(ChatMessage.MsgBody.newBuilder()
                    .setMessageType(ChatMessage.MessageType.SYSTEM)
                    .setTextContent(ChatMessage.TextContent.newBuilder().setText("msg type not supported")).build().toByteArray())));
            return;
        }

        Channel channel = ctx.channel();
        switch (msgBody.getMessageType()) {
            case TEXT:
                // 文本消息的处理
                handleTextMessage(channel, msgBody);
                break;
            case IMAGE:
                // 图片消息的处理
                handleImageMessage(channel, msgBody);
                break;
            case PING:
                logger.info("receive ping message");
                return;
            case PONG:
                UserInfoManager.updateUserTime(channel);
                logger.info("receive pong message, address: {}", NettyUtil.parseChannelRemoteAddr(channel));
                return;
            case AUTH:
                handleAuthMessage(channel, msgBody);
                return;
            default:
                logger.warn("The message type [{}] can't be auth!!!", msgBody.getMessageType());
                return;
        }
        // 交给下一个处理器
        ctx.fireChannelRead(frame.retain());
    }

    private void handleTextMessage(Channel channel, ChatMessage.MsgBody msgBody) {
        logger.info("Received TEXT message: {}", msgBody.getTextContent().getText());
        // 这里可以处理文本消息，或者交给消息处理器进一步处理
    }

    private void handleImageMessage(Channel channel, ChatMessage.MsgBody msgBody) {
        logger.info("Received IMAGE message: {}", msgBody.getImageContent().getImageUrl());
        // 这里可以处理图片消息
    }

    private void handleAuthMessage(Channel channel, ChatMessage.MsgBody msgBody) {
        String userId = msgBody.getSender().getUserId();
        String username = msgBody.getSender().getUsername();
        String password = msgBody.getSender().getAvatarUrl();
        boolean isSuccess = UserInfoManager.saveUser(channel, username, password);
        UserInfoManager.sendInfo(channel, ChatCode.SYS_AUTH_STATE, isSuccess);
        if (isSuccess) {
            UserInfoManager.broadCastInfo(ChatCode.SYS_USER_COUNT, UserInfoManager.getAuthUserCount());
        }
    }
}
