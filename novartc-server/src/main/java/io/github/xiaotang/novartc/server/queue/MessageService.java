package io.github.xiaotang.novartc.server.queue;

import io.github.xiaotang.novartc.server.proto.ChatMessage;

/**
 * Created on 2026/3/10 13:45
 *
 * @author tangj
 * @description
 */
public interface MessageService {
    void sendMessage(ChatMessage.MsgBody message);
}
