package io.github.xiaotang.novartc.server.repository.mongodb.impl;

import io.github.xiaotang.novartc.server.entity.mongodb.IMUser;
import io.github.xiaotang.novartc.server.repository.mongodb.IMUserRepository;
import io.github.xiaotang.novartc.server.repository.mongodb.BaseMongoDaoIMpl;
import org.springframework.stereotype.Repository;

/**
 * Created on 2026/2/27 17:33
 *
 * @author tangj
 * @description
 */
@Repository
public class IMUserRepositoryImpl extends BaseMongoDaoIMpl<IMUser> implements IMUserRepository {

    protected IMUserRepositoryImpl() {
        super(IMUser.class);
    }
}
