package io.github.xiaotang.novartc.server.repository.mongodb;

import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;

import java.util.List;
import java.util.Map;

/**
 * Created on 2026/3/24 10:20
 *
 * @author tangj
 * @description
 */
public interface IMMessageRepository extends BaseMongoDao<IMMessage> {
    List<IMMessage> findHistory(String chatId, Long beforeChatSeq, int limit);

    Map<String, IMMessage> findLatestByChatIds(List<String> chatIds);

    List<String> findOtherUserMessageIds(String chatId, String currentUserId);

    Map<String, List<String>> findOtherUserMessageIdsByChatIds(List<String> chatIds, String currentUserId);

    IMMessage findByMessageId(String messageId);
}
