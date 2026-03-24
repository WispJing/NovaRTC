package io.github.xiaotang.novartc.server.repository.mongodb;

import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;

import java.util.List;

/**
 * Created on 2026/3/24 10:20
 *
 * @author tangj
 * @description
 */
public interface IMMessageRepository extends BaseMongoDao<IMMessage> {
    List<IMMessage> findHistory(String chatId, Long beforeChatSeq, int limit);

    List<String> findOtherUserMessageIds(String chatId, String currentUserId);
}
