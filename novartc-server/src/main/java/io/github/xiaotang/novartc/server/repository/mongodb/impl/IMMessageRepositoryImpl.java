package io.github.xiaotang.novartc.server.repository.mongodb.impl;

import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.repository.mongodb.BaseMongoDaoIMpl;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public Map<String, IMMessage> findLatestByChatIds(List<String> chatIds) {
        if (chatIds == null || chatIds.isEmpty()) {
            return Map.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("chat_id").in(chatIds)),
                Aggregation.sort(Sort.by(
                        Sort.Order.asc("chat_id"),
                        Sort.Order.desc("chat_seq")
                )),
                Aggregation.group("chat_id").first(Aggregation.ROOT).as("latest_message"),
                Aggregation.replaceRoot("latest_message")
        );
        return aggregate(aggregation, IMMessage.class).stream()
                .filter(message -> message.getChatId() != null)
                .collect(Collectors.toMap(
                        IMMessage::getChatId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
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
    public Map<String, List<String>> findOtherUserMessageIdsByChatIds(List<String> chatIds,
                                                                        String currentUserId) {
        if (chatIds == null || chatIds.isEmpty()) {
            return Map.of();
        }

        Query query = Query.query(Criteria.where("chat_id").in(chatIds).and("sender_id").ne(currentUserId));
        query.fields().include("chat_id").include("message_id");
        return find(query).stream()
                .filter(message -> message.getChatId() != null && message.getMessageId() != null
                        && !message.getMessageId().isBlank())
                .collect(Collectors.groupingBy(
                        IMMessage::getChatId,
                        LinkedHashMap::new,
                        Collectors.mapping(IMMessage::getMessageId, Collectors.toList())
                ));
    }

    @Override
    public IMMessage findByMessageId(String messageId) {
        return findOne(Query.query(Criteria.where("message_id").is(messageId)));
    }
}
