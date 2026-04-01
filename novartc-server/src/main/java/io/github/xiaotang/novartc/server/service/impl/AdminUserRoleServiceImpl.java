package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminUserRole;
import io.github.xiaotang.novartc.server.mapper.AdminUserRoleMapper;
import io.github.xiaotang.novartc.server.service.IAdminUserRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 后台用户角色服务实现，提供用户与角色关系的基础数据操作能力。
 */
@Service
public class AdminUserRoleServiceImpl extends ServiceImpl<AdminUserRoleMapper, AdminUserRole> implements IAdminUserRoleService {

}
