package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.common.enums.ExceptionEnum;
import io.github.xiaotang.novartc.server.admin.service.RiskControlService;
import io.github.xiaotang.novartc.server.config.ChatMsgLog;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import io.github.xiaotang.novartc.server.service.MessagePersistenceService;
import io.github.xiaotang.novartc.server.service.UserRelationCacheService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class MessageHandler extends SimpleChannelInboundHandler<ChatMessage.MsgBody> {
    private final ChatDispatchService chatDispatchService;
    private final MessagePersistenceService messagePersistenceService;
    private final UserRelationCacheService userRelationCacheService;
    private final RiskControlService riskControlService;
    private final ImMessageWorkerPool imMessageWorkerPool;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                ChatMessage.MsgBody msgBody) {

        Channel channel = ctx.channel();

        String userId = channel.attr(NettyAttributes.USER_ID).get();

        if (userId == null) {
            ChatMsgLog.msgLog.warn("unauthorized message: {}", msgBody);
            channel.close();
            return;
        }

        switch (msgBody.getMessageType()) {
            case PING -> ChatMsgLog.msgLog.info("receive ping {}", userId);

            case PONG -> ChatMsgLog.msgLog.info("receive pong {}", userId);

            // 业务处理（风控、持久化、分发）移交业务线程池，避免阻塞 Netty IO 线程；
            // 按发送者分桶保证同一用户的消息严格串行
            case TEXT, IMAGE, VIDEO, AUDIO, FILE, TEXT_AND_IMAGE ->
                    imMessageWorkerPool.submit(userId, () -> handleMessage(ctx, msgBody, userId));

            default -> ChatMsgLog.msgLog.warn("unsupported type {}, from {}", msgBody.getMessageType(), userId);
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, ChatMessage.MsgBody msgBody, String userId) {
        try {
            String riskMessage = riskControlService.validateSendMessage(userId, msgBody);
            if (StringUtils.isNotBlank(riskMessage)) {
                ChatMsgLog.msgLog.warn("message blocked by risk control, sender {}, reason {}", userId, riskMessage);
                WebSocketMsgHandler.sendSystemMessage(ctx, riskMessage);
                return;
            }

            if (!canSendMessage(userId, msgBody)) {
                ChatMsgLog.msgLog.warn("message blocked because sender {} and target {} are not friends",
                        userId,
                        msgBody.hasSingleChatInfo() ? msgBody.getSingleChatInfo().getUser2Id() : "");
                WebSocketMsgHandler.sendSystemMessage(ctx, ExceptionEnum.FRIEND_CHAT_ONLY_FOR_FRIENDS.getContent());
                return;
            }

            ChatMessage.MsgBody persistedMessage = messagePersistenceService.persist(msgBody, userId);
            ChatMsgLog.msg(persistedMessage);
            // 将服务端生成的雪花 ID 回传当前连接，前端据此替换发送中的临时消息。
            ctx.writeAndFlush(persistedMessage);
            chatDispatchService.dispatch(persistedMessage);
        } catch (Exception e) {
            ChatMsgLog.msgLog.error("消息处理异常: userId={}", userId, e);
            WebSocketMsgHandler.sendSystemMessage(ctx, "消息处理失败，请稍后重试");
        }
    }

    private boolean canSendMessage(String currentUserId, ChatMessage.MsgBody msgBody) {
        if (msgBody.getIsGroup()) {
            return true;
        }
        if (!msgBody.hasSingleChatInfo()) {
            return false;
        }

        String targetUserId = msgBody.getSingleChatInfo().getUser2Id();
        if (StringUtils.isBlank(targetUserId)) {
            return false;
        }
        return userRelationCacheService.isFriend(currentUserId, targetUserId);
    }
}
