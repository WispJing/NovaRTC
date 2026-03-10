package io.github.xiaotang.novartc.server.queue.local;

import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Created on 2026/3/10 13:46
 *
 * @author tangj
 * @description
 */
@Service
@ConditionalOnMissingBean(name = "rocketMQTemplate")
@RequiredArgsConstructor
public class LocalMessageService implements MessageService {
    private final UserChannelManager userChannelManager;

    @Override
    public void sendMessage(ChatMessage.MsgBody message) {
        // 根据消息类型解析接收者
        if (message.getIsGroup()) {
            String groupId = message.getGroupChatInfo().getGroupId();
            Set<String> members = userChannelManager.getGroupMembers(groupId);
            for (String userId : members) {
                userChannelManager.sendToUser(userId, message);
            }
        } else {
            String targetUserId = message.getSingleChatInfo().getUser2Id();
            userChannelManager.sendToUser(targetUserId, message);
        }
    }
}
