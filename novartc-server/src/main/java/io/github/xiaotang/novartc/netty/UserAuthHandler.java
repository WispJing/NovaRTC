package io.github.xiaotang.novartc.netty;

import io.github.xiaotang.novartc.proto.ChatMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2026/2/28 14:32
 *
 * @author tangj
 * @description
 */
public class UserAuthHandler extends SimpleChannelInboundHandler<ChatMessage.MsgBody> {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                ChatMessage.MsgBody msgBody) {

        Channel channel = ctx.channel();

        // 只处理 AUTH
        if (msgBody.getMessageType() != ChatMessage.MessageType.AUTH) {

            // 未认证用户直接关闭
            if (channel.attr(NettyAttributes.USER_ID).get() == null) {
                logger.warn("unauthorized connection {}", channel.remoteAddress());
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
            logger.warn("auth failed {}", userId);
            channel.close();
            return;
        }

        // 绑定 Channel Attribute
        channel.attr(NettyAttributes.USER_ID).set(userId);

        // 绑定用户 Channel
        UserChannelManager.bind(userId, channel);

        logger.info("user login success {}", userId);

        // 认证成功后移除 AuthHandler
        ctx.pipeline().remove(this);
    }

    private boolean verifyToken(String userId, String token) {

        // 示例逻辑
        return token != null && !token.isEmpty();
    }

}
