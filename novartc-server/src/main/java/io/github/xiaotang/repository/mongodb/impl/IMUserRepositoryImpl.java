package io.github.xiaotang.repository.mongodb.impl;

import io.github.xiaotang.entity.mongodb.IMUser;
import io.github.xiaotang.repository.mongodb.BaseMongoDaoIMpl;
import io.github.xiaotang.repository.mongodb.IMUserRepository;
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
