package io.github.xiaotang.novartc.server.queue;

import io.github.xiaotang.novartc.server.proto.ChatMessage;

/**
 * 在线状态派发服务
 */
public interface PresenceDispatchService {
    void dispatch(ChatMessage.MsgBody message);
}
