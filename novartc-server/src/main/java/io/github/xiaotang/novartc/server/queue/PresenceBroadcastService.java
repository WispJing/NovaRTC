package io.github.xiaotang.novartc.server.queue;

import io.github.xiaotang.novartc.server.proto.ChatMessage;

/**
 * 在线状态广播服务
 */
public interface PresenceBroadcastService {
    void broadcast(ChatMessage.MsgBody message);
}
