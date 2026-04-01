package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.proto.ChatMessage;

/**
 * 消息持久化服务，负责写入消息存储并补全服务端元数据。
 */
public interface MessagePersistenceService {
    /**
     * 持久化消息并返回补全后的消息体。
     */
    ChatMessage.MsgBody persist(ChatMessage.MsgBody message, String authenticatedUserId);
}
