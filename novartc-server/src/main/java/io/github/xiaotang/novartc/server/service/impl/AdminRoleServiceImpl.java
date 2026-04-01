package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminRole;
import io.github.xiaotang.novartc.server.mapper.AdminRoleMapper;
import io.github.xiaotang.novartc.server.service.IAdminRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 后台角色表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements IAdminRoleService {

}
