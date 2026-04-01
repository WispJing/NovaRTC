package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminMenu;
import io.github.xiaotang.novartc.server.mapper.AdminMenuMapper;
import io.github.xiaotang.novartc.server.service.IAdminMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 后台菜单服务实现，提供后台菜单的基础数据操作能力。
 */
@Service
public class AdminMenuServiceImpl extends ServiceImpl<AdminMenuMapper, AdminMenu> implements IAdminMenuService {

}
