package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.dto.IMUserDTO;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.mapper.ImUserMapper;
import io.github.xiaotang.novartc.server.service.IImUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现，负责用户信息查询与基础数据操作。
 */
@Service
public class ImUserServiceImpl extends ServiceImpl<ImUserMapper, ImUser> implements IImUserService {
    /**
     * 按用户主键查询有效用户。
     */
    @Override
    public ImUser selectByUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<ImUser>()
                .eq(ImUser::getIsDeleted, false)
                .eq(ImUser::getId, userId));
    }

    /**
     * 按用户编号查询有效用户。
     */
    @Override
    public ImUser selectByUserNum(String userNum) {
        if (StringUtils.isBlank(userNum)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<ImUser>()
                .eq(ImUser::getIsDeleted, false)
                .eq(ImUser::getUserNum, userNum));
    }

    /**
     * 按邮箱查询有效用户。
     */
    @Override
    public ImUser selectByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<ImUser>()
                .eq(ImUser::getIsDeleted, false)
                .eq(ImUser::getEmail, email));
    }
}
