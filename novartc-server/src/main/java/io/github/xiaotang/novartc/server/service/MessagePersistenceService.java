package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.proto.ChatMessage;

/**
 * Created on 2026/3/24 10:32
 *
 * @author tangj
 * @description
 */
public interface MessagePersistenceService {
    ChatMessage.MsgBody persist(ChatMessage.MsgBody message, String authenticatedUserId);
}
