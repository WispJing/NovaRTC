package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminUserMenu;
import io.github.xiaotang.novartc.server.mapper.AdminUserMenuMapper;
import io.github.xiaotang.novartc.server.service.IAdminUserMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 后台用户菜单授权服务实现，提供用户直授权菜单关系的基础数据操作能力。
 */
@Service
public class AdminUserMenuServiceImpl extends ServiceImpl<AdminUserMenuMapper, AdminUserMenu> implements IAdminUserMenuService {

}
