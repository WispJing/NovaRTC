package io.github.xiaotang.novartc.server.queue;

import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;

/**
 * token 踢下线派发服务
 */
public interface TokenKickDispatchService {
    void dispatch(TokenKickMessage message);
}
