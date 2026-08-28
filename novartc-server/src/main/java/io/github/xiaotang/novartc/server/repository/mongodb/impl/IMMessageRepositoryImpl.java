package io.github.xiaotang.novartc.server.repository.mongodb.impl;

import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.repository.mongodb.BaseMongoDaoIMpl;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created on 2026/3/24 10:20
 *
 * @author tangj
 * @description
 */
@Repository
public class IMMessageRepositoryImpl extends BaseMongoDaoIMpl<IMMessage> implements IMMessageRepository {

    protected IMMessageRepositoryImpl() {
        super(IMMessage.class);
    }

    @Override
    public List<IMMessage> findHistory(String chatId, Long beforeChatSeq, int limit) {
        Criteria criteria = Criteria.where("chat_id").is(chatId);
        if (beforeChatSeq != null) {
            criteria = criteria.and("chat_seq").lt(beforeChatSeq);
        }

        Query query = Query.query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "chat_seq"))
                .limit(limit);

        return find(query);
    }

    @Override
    public List<String> findOtherUserMessageIds(String chatId, String currentUserId) {
        Query query = Query.query(Criteria.where("chat_id").is(chatId).and("sender_id").ne(currentUserId));
        query.fields().include("message_id");
        return find(query).stream()
                .map(IMMessage::getMessageId)
                .filter(messageId -> messageId != null && !messageId.isBlank())
                .toList();
    }

    @Override
    public IMMessage findByMessageId(String messageId) {
        return findOne(Query.query(Criteria.where("message_id").is(messageId)));
    }
}
