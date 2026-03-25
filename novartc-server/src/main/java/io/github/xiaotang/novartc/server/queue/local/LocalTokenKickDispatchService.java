package io.github.xiaotang.novartc.server.queue.local;

import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.queue.TokenKickDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalTokenKickDispatchService implements TokenKickDispatchService {
    private final ObjectProvider<UserChannelManager> userChannelManagerProvider;

    @Override
    public void dispatch(TokenKickMessage message) {
        userChannelManagerProvider.getObject().closeByToken(message.getToken());
    }
}
