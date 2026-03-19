package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAccessLog;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.service.LoginService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
    private final LoginService loginService;
    private final IImUserService imUserService;

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

        ImUser imUser = imUserService.selectByUserId(userId);
        if (imUser == null) {
            handleAuthFailed(channel, userId);
            return;
        }
        userId = imUser.getId();

        String tokenUserId;
        try {
            tokenUserId = loginService.checkAndRefresh(token);
        } catch (Exception e) {
            tokenUserId = null;
        }

        if (tokenUserId == null || !Objects.equals(userId, tokenUserId)) {
            handleAuthFailed(channel, userId);
            return;
        }

        // 绑定用户 Channel
        userChannelManager.bind(userId, token, channel);

        ChatAccessLog.log.info("user login success {}", imUser.getUserNum());

        sendAuthResult(channel, true, "user login success");
        // 认证成功后移除 AuthHandler
        ctx.pipeline().remove(this);
    }

    private void handleAuthFailed(Channel channel, String userId) {
        ChatAccessLog.log.warn("auth failed {}", userId);
        sendAuthResult(channel, false, "auth failed");
        channel.close();
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
        userChannelManager.remove(ctx.channel());
        String userId = ctx.channel().attr(NettyAttributes.USER_ID).get();
        if (StringUtils.isNotBlank(userId)) {
            ChatAccessLog.log.info("用户下线 {}", userId);
        }
    }

}
