package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminUserRole;
import io.github.xiaotang.novartc.server.mapper.AdminUserRoleMapper;
import io.github.xiaotang.novartc.server.service.IAdminUserRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 后台用户角色关系表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Service
public class AdminUserRoleServiceImpl extends ServiceImpl<AdminUserRoleMapper, AdminUserRole> implements IAdminUserRoleService {

}
