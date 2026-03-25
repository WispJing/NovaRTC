package io.github.xiaotang.novartc.server.queue;

import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;

/**
 * token 踢下线广播服务
 */
public interface TokenKickBroadcastService {
    void broadcast(TokenKickMessage message);
}
