package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminOperateLog;
import io.github.xiaotang.novartc.server.mapper.AdminOperateLogMapper;
import io.github.xiaotang.novartc.server.service.IAdminOperateLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 后台操作日志表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Service
public class AdminOperateLogServiceImpl extends ServiceImpl<AdminOperateLogMapper, AdminOperateLog> implements IAdminOperateLogService {

}
