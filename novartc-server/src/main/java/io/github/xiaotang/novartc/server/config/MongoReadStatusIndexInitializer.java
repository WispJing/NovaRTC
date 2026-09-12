package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.server.entity.mongodb.IMReadStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * 为按用户批量查询已读消息提供索引。
 * <p>
 * ensureIndex 幂等执行；索引初始化失败不阻断服务启动，但会保留告警以便运维处理。
 */
@Component
@RequiredArgsConstructor
public class MongoReadStatusIndexInitializer {
    private static final Logger logger = LoggerFactory.getLogger(MongoReadStatusIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureReadStatusIndex() {
        try {
            String indexName = mongoTemplate.indexOps(IMReadStatus.class).ensureIndex(
                    new Index()
                            .on("user_id", Sort.Direction.ASC)
                            .on("message_id", Sort.Direction.ASC)
                            .named("idx_user_message")
            );
            logger.info("MongoDB 已读状态索引就绪: {}", indexName);
        } catch (Exception e) {
            logger.warn("MongoDB 已读状态索引初始化失败，批量未读查询可能退化: idx_user_message", e);
        }
    }
}
