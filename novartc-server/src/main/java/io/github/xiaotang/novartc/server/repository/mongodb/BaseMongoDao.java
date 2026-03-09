package io.github.xiaotang.novartc.server.repository.mongodb;

/**
 * Created on 2026/2/27 17:47
 *
 * @author tangj
 * @description
 */

import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

/**
 * BaseMongoDao 接口定义通用 MongoDB CRUD、分页、更新和聚合操作
 *
 * @param <T> 实体类型
 * @author tangj
 */
public interface BaseMongoDao<T> {

    /* ===================== 基础 CRUD ===================== */

    void insert(T entity);

    void insert(T entity, String collectionName);

    void insertBatch(List<T> list);

    void save(T entity);

    T findById(Object id);

    List<T> findAll();

    void deleteById(Object id);

    void delete(Query query);

    /* ===================== 查询 ===================== */

    List<T> find(Query query);

    T findOne(Query query);

    long count(Query query);

    boolean exists(Query query);

    /* ===================== 分页查询 ===================== */

    Page<T> page(Query query, int page, int size);

    /* ===================== 更新 ===================== */

    long update(Query query, Update update);

    long updateOne(Query query, Update update);

    /* ===================== 聚合 ===================== */

    <R> List<R> aggregate(Aggregation aggregation, Class<R> resultClass);

}
