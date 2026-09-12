package io.github.xiaotang.novartc.server.repository.mongodb.impl;

import io.github.xiaotang.novartc.server.entity.mongodb.IMReadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IMReadStatusRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private IMReadStatusRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new IMReadStatusRepositoryImpl();
        ReflectionTestUtils.setField(repository, "mongoTemplate", mongoTemplate);
    }

    @Test
    void findReadMessageIdsShouldSplitLargeInQueryAndMergeResults() {
        IMReadStatus first = readStatus("message-1");
        IMReadStatus second = readStatus("message-1001");
        when(mongoTemplate.find(any(Query.class), eq(IMReadStatus.class)))
                .thenReturn(List.of(first), List.of(second));
        List<String> messageIds = java.util.stream.IntStream.rangeClosed(1, 1001)
                .mapToObj(index -> "message-" + index)
                .toList();

        Set<String> result = repository.findReadMessageIds("user-1", messageIds);

        assertThat(result).containsExactly("message-1", "message-1001");
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(2)).find(queryCaptor.capture(), eq(IMReadStatus.class));
        assertThat(queryCaptor.getAllValues())
                .allSatisfy(query -> assertThat(query.getQueryObject().get("user_id")).isEqualTo("user-1"));
    }

    private IMReadStatus readStatus(String messageId) {
        IMReadStatus status = new IMReadStatus();
        status.setMessage_id(messageId);
        return status;
    }
}
