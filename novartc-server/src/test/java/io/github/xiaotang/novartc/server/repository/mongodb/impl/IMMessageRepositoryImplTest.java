package io.github.xiaotang.novartc.server.repository.mongodb.impl;

import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IMMessageRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private IMMessageRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new IMMessageRepositoryImpl();
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);
    }

    @Test
    void findLatestByChatIdsShouldReturnMessagesByChatWithOneAggregation() {
        IMMessage first = message("chat-1", "message-1");
        IMMessage second = message("chat-2", "message-2");
        when(mongoTemplate.getCollectionName(IMMessage.class)).thenReturn("message");
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("message"), eq(IMMessage.class)))
                .thenReturn(new AggregationResults<>(List.of(first, second), new Document()));

        Map<String, IMMessage> result = repository.findLatestByChatIds(List.of("chat-1", "chat-2"));

        assertThat(result).containsExactly(
                Map.entry("chat-1", first),
                Map.entry("chat-2", second)
        );
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("message"), eq(IMMessage.class));
    }

    @Test
    void findLatestByChatIdsShouldSkipMongoForEmptyInput() {
        assertThat(repository.findLatestByChatIds(List.of())).isEmpty();

        verify(mongoTemplate, never()).aggregate(
                any(Aggregation.class), any(String.class), eq(IMMessage.class));
    }

    @Test
    void findOtherUserMessageIdsByChatIdsShouldGroupProjectedMessages() {
        IMMessage chatOneFirst = message("chat-1", "message-1");
        IMMessage chatTwo = message("chat-2", "message-2");
        IMMessage chatOneSecond = message("chat-1", "message-3");
        when(mongoTemplate.find(any(Query.class), eq(IMMessage.class)))
                .thenReturn(List.of(chatOneFirst, chatTwo, chatOneSecond));

        Map<String, List<String>> result = repository.findOtherUserMessageIdsByChatIds(
                List.of("chat-1", "chat-2"), "user-1");

        assertThat(result).containsExactly(
                Map.entry("chat-1", List.of("message-1", "message-3")),
                Map.entry("chat-2", List.of("message-2"))
        );
        verify(mongoTemplate).find(any(Query.class), eq(IMMessage.class));
    }

    private IMMessage message(String chatId, String messageId) {
        IMMessage message = new IMMessage();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        return message;
    }
}
