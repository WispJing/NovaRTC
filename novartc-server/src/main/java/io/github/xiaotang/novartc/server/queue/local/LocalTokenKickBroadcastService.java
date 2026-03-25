package io.github.xiaotang.novartc.server.queue.local;

import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.queue.TokenKickBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalTokenKickBroadcastService implements TokenKickBroadcastService {
    private final UserChannelManager userChannelManager;

    @Override
    public void broadcast(TokenKickMessage message) {
        userChannelManager.closeByToken(message.getToken());
    }
}
