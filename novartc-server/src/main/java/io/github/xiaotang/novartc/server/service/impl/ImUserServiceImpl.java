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
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
@Service
public class ImUserServiceImpl extends ServiceImpl<ImUserMapper, ImUser> implements IImUserService {
    @Override
    public ImUser selectByUserId(String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<ImUser>()
                .eq(ImUser::getIsDeleted, false)
                .eq(ImUser::getId, userId));
    }

    @Override
    public ImUser selectByUserNum(String userNum) {
        if (StringUtils.isBlank(userNum)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<ImUser>()
                .eq(ImUser::getIsDeleted, false)
                .eq(ImUser::getUserNum, userNum));
    }
}
