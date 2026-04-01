package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.IMUserDTO;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务，负责用户信息查询与基础数据操作。
 */
public interface IImUserService extends IService<ImUser> {
    /**
     * 按用户主键查询用户信息。
     */
    ImUser selectByUserId(String userId);

    /**
     * 按用户编号查询用户信息。
     */
    ImUser selectByUserNum(String userNum);
}
