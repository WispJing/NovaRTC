package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.server.entity.mongodb.IMReadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoReadStatusIndexInitializerTest {

    @Test
    void ensureReadStatusIndexShouldCreateNamedCompoundIndex() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(mongoTemplate.indexOps(IMReadStatus.class)).thenReturn(indexOperations);
        when(indexOperations.ensureIndex(org.mockito.ArgumentMatchers.any())).thenReturn("idx_user_message");

        new MongoReadStatusIndexInitializer(mongoTemplate).ensureReadStatusIndex();

        verify(mongoTemplate).indexOps(IMReadStatus.class);
        verify(indexOperations).ensureIndex(org.mockito.ArgumentMatchers.any());
    }
}
