package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminUserMenu;
import io.github.xiaotang.novartc.server.mapper.AdminUserMenuMapper;
import io.github.xiaotang.novartc.server.service.IAdminUserMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 后台用户菜单直授权关系表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Service
public class AdminUserMenuServiceImpl extends ServiceImpl<AdminUserMenuMapper, AdminUserMenu> implements IAdminUserMenuService {

}
