package io.github.xiaotang.novartc.server.queue.local;

import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.PresenceBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalPresenceBroadcastService implements PresenceBroadcastService {
    private final UserChannelManager userChannelManager;

    @Override
    public void broadcast(ChatMessage.MsgBody message) {
        userChannelManager.sendToAllUsers(message);
    }
}
