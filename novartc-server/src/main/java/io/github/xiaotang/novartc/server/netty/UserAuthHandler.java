package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created on 2026/2/28 14:32
 *
 * @author tangj
 * @description
 */
@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class UserAuthHandler extends SimpleChannelInboundHandler<ChatMessage.MsgBody> {
    private final UserChannelManager userChannelManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                ChatMessage.MsgBody msgBody) {

        Channel channel = ctx.channel();

        // 只处理 AUTH
        if (msgBody.getMessageType() != ChatMessage.MessageType.AUTH) {

            // 未认证用户直接关闭
            if (channel.attr(NettyAttributes.USER_ID).get() == null) {
                ChatAccessLog.log.warn("unauthorized connection {}", channel.remoteAddress());
                channel.close();
                return;
            }

            ctx.fireChannelRead(msgBody);
            return;
        }

        handleAuthMessage(channel, msgBody, ctx);
    }

    private void handleAuthMessage(Channel channel,
                                   ChatMessage.MsgBody msgBody,
                                   ChannelHandlerContext ctx) {

        ChatMessage.Auth auth = msgBody.getAuthMessage();

        String userId = auth.getUserId();
        String token = auth.getToken();

        // TODO 真实环境这里需要调用登录服务校验 token
        boolean success = verifyToken(userId, token);

        if (!success) {
            ChatAccessLog.log.warn("auth failed {}", userId);
            sendAuthResult(channel, false, "auth failed");
            channel.close();
            return;
        }

        // 绑定用户 Channel
        userChannelManager.bind(channel);

        ChatAccessLog.log.info("user login success {}", userId);

        sendAuthResult(channel, true, "user login success");
        // 认证成功后移除 AuthHandler
        ctx.pipeline().remove(this);
    }

    private boolean verifyToken(String userId, String token) {

        // 示例逻辑
        return token != null && !token.isEmpty();
    }

    private void sendAuthResult(Channel channel, Boolean isSuccess, String message) {

        ChatMessage.MsgBody authResult =
                ChatMessage.MsgBody.newBuilder()
                        .setMessageType(ChatMessage.MessageType.AUTH)
                        .setAuthMessage(
                                ChatMessage.Auth.newBuilder()
                                        .setSuccess(isSuccess)
                                        .setMessage(message)
                                        .build()
                        )
                        .build();

        channel.writeAndFlush(authResult);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String userId = ctx.channel().attr(NettyAttributes.USER_ID).get();
        userChannelManager.remove(ctx.channel());
        ChatAccessLog.log.info("用户下线 {}", userId);
    }

}
