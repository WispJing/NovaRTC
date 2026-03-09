package io.github.xiaotang.novartc.server.repository.mongodb;

import com.mongodb.client.result.UpdateResult;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

/**
 * Created on 2026/2/27 17:34
 *
 * @author tangj
 * @description
 */
@Getter
public abstract class BaseMongoDaoIMpl<T> implements BaseMongoDao<T> {
    @Autowired
    protected MongoTemplate mongoTemplate;

    private final Class<T> entityClass;

    protected BaseMongoDaoIMpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /* ===================== 基础 CRUD ===================== */

    public void insert(T entity) {
        mongoTemplate.insert(entity);
    }

    public void insert(T entity, String collectionName) {
        mongoTemplate.insert(entity, collectionName);
    }

    public void insertBatch(List<T> list) {
        mongoTemplate.insert(list, entityClass);
    }

    public void save(T entity) {
        mongoTemplate.save(entity);
    }

    public T findById(Object id) {
        return mongoTemplate.findById(id, entityClass);
    }

    public List<T> findAll() {
        return mongoTemplate.findAll(entityClass);
    }

    public void deleteById(Object id) {
        Query query = Query.query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, entityClass);
    }

    public void delete(Query query) {
        mongoTemplate.remove(query, entityClass);
    }

    /* ===================== 查询 ===================== */

    public List<T> find(Query query) {
        return mongoTemplate.find(query, entityClass);
    }

    public T findOne(Query query) {
        return mongoTemplate.findOne(query, entityClass);
    }

    public long count(Query query) {
        return mongoTemplate.count(query, entityClass);
    }

    public boolean exists(Query query) {
        return mongoTemplate.exists(query, entityClass);
    }

    /* ===================== 分页查询 ===================== */

    public Page<T> page(Query query, int page, int size) {

        long total = count(query);

        query.with(PageRequest.of(page, size));

        List<T> list = mongoTemplate.find(query, entityClass);

        return new PageImpl<>(list, PageRequest.of(page, size), total);
    }

    /* ===================== 更新 ===================== */

    public long update(Query query, Update update) {
        UpdateResult result =
                mongoTemplate.updateMulti(query, update, entityClass);
        return result.getModifiedCount();
    }

    public long updateOne(Query query, Update update) {
        UpdateResult result =
                mongoTemplate.updateFirst(query, update, entityClass);
        return result.getModifiedCount();
    }

    /* ===================== 聚合 ===================== */

    public <R> List<R> aggregate(
            Aggregation aggregation,
            Class<R> resultClass) {

        AggregationResults<R> results =
                mongoTemplate.aggregate(aggregation,
                        mongoTemplate.getCollectionName(entityClass),
                        resultClass);

        return results.getMappedResults();
    }

    /* ===================== 动态集合支持 ===================== */

    protected String getCollectionName() {
        return mongoTemplate.getCollectionName(entityClass);
    }
}
