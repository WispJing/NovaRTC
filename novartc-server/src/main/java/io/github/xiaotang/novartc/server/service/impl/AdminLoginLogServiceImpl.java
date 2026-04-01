package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminLoginLog;
import io.github.xiaotang.novartc.server.mapper.AdminLoginLogMapper;
import io.github.xiaotang.novartc.server.service.IAdminLoginLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 后台登录日志服务实现，提供后台登录日志的基础数据操作能力。
 */
@Service
public class AdminLoginLogServiceImpl extends ServiceImpl<AdminLoginLogMapper, AdminLoginLog> implements IAdminLoginLogService {

}
