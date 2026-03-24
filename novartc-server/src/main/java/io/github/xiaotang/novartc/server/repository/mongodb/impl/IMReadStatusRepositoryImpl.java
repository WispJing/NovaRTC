package io.github.xiaotang.novartc.server.repository.mongodb.impl;

import io.github.xiaotang.novartc.server.entity.mongodb.IMReadStatus;
import io.github.xiaotang.novartc.server.repository.mongodb.BaseMongoDaoIMpl;
import io.github.xiaotang.novartc.server.repository.mongodb.IMReadStatusRepository;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on 2026/3/24 12:01
 *
 * @author tangj
 * @description
 */
@Repository
public class IMReadStatusRepositoryImpl extends BaseMongoDaoIMpl<IMReadStatus> implements IMReadStatusRepository {

    protected IMReadStatusRepositoryImpl() {
        super(IMReadStatus.class);
    }

    @Override
    public Set<String> findReadMessageIds(String userId, List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Set.of();
        }

        Query query = Query.query(Criteria.where("user_id").is(userId).and("message_id").in(messageIds));
        return find(query).stream()
                .map(IMReadStatus::getMessage_id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
