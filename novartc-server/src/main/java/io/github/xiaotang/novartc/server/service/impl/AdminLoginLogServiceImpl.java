package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminLoginLog;
import io.github.xiaotang.novartc.server.mapper.AdminLoginLogMapper;
import io.github.xiaotang.novartc.server.service.IAdminLoginLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 后台登录日志表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Service
public class AdminLoginLogServiceImpl extends ServiceImpl<AdminLoginLogMapper, AdminLoginLog> implements IAdminLoginLogService {

}
