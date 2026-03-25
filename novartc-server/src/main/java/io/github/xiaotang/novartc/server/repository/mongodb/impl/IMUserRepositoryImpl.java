package io.github.xiaotang.novartc.server.repository.mongodb.impl;

import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.repository.mongodb.BaseMongoDaoIMpl;
import io.github.xiaotang.novartc.server.repository.mongodb.IMUserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class IMUserRepositoryImpl extends BaseMongoDaoIMpl<ImUser> implements IMUserRepository {

    protected IMUserRepositoryImpl() {
        super(ImUser.class);
    }
}
