package io.github.xiaotang.novartc.server.queue.local;

import io.github.xiaotang.novartc.server.netty.LocalSessionDispatcher;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.PresenceDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalPresenceDispatchService implements PresenceDispatchService {
    private final LocalSessionDispatcher localSessionDispatcher;

    @Override
    public void dispatch(ChatMessage.MsgBody message, Set<String> targetUserIds) {
        localSessionDispatcher.sendToUsers(targetUserIds, message);
    }
}
