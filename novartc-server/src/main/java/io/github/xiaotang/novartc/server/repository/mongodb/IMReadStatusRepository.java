package io.github.xiaotang.novartc.server.repository.mongodb;

import io.github.xiaotang.novartc.server.entity.mongodb.IMReadStatus;

import java.util.List;
import java.util.Set;

/**
 * Created on 2026/3/24 12:00
 *
 * @author tangj
 * @description
 */
public interface IMReadStatusRepository extends BaseMongoDao<IMReadStatus> {
    Set<String> findReadMessageIds(String userId, List<String> messageIds);
}
