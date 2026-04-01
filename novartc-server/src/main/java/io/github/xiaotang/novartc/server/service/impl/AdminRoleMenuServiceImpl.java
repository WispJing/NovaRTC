package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminRoleMenu;
import io.github.xiaotang.novartc.server.mapper.AdminRoleMenuMapper;
import io.github.xiaotang.novartc.server.service.IAdminRoleMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 后台角色菜单服务实现，提供角色与菜单关系的基础数据操作能力。
 */
@Service
public class AdminRoleMenuServiceImpl extends ServiceImpl<AdminRoleMenuMapper, AdminRoleMenu> implements IAdminRoleMenuService {

}
