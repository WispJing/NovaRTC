package io.github.xiaotang.novartc.server.netty;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.enums.ExceptionEnum;
import io.github.xiaotang.novartc.server.config.ChatMsgLog;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriend;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import io.github.xiaotang.novartc.server.service.IImFriendService;
import io.github.xiaotang.novartc.server.service.MessagePersistenceService;
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
    private final IImFriendService imFriendService;

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

            case TEXT, IMAGE, VIDEO, AUDIO, FILE, TEXT_AND_IMAGE -> handleMessage(ctx, msgBody, userId);

            default -> ChatMsgLog.msgLog.warn("unsupported type {}, from {}", msgBody.getMessageType(), userId);
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, ChatMessage.MsgBody msgBody, String userId) {
        if (!canSendMessage(userId, msgBody)) {
            ChatMsgLog.msgLog.warn("message blocked because sender {} and target {} are not friends",
                    userId,
                    msgBody.hasSingleChatInfo() ? msgBody.getSingleChatInfo().getUser2Id() : "");
            WebSocketMsgHandler.sendSystemMessage(ctx, ExceptionEnum.FRIEND_CHAT_ONLY_FOR_FRIENDS.getContent());
            return;
        }

        ChatMessage.MsgBody persistedMessage = messagePersistenceService.persist(msgBody, userId);
        ChatMsgLog.msg(persistedMessage);
        chatDispatchService.dispatch(persistedMessage);
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
        if (StringUtils.equals(currentUserId, targetUserId)) {
            return true;
        }

        return imFriendService.count(new LambdaQueryWrapper<ImFriend>()
                .eq(ImFriend::getIsDeleted, false)
                .eq(ImFriend::getUserId, currentUserId)
                .eq(ImFriend::getFriendId, targetUserId)) > 0;
    }
}
