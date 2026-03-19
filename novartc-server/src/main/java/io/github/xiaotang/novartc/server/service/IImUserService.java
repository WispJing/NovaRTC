package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.IMUserDTO;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户信息表 服务类
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
public interface IImUserService extends IService<ImUser> {
    ImUser selectByUserId(String userId);
    ImUser selectByUserNum(String userNum);
}
