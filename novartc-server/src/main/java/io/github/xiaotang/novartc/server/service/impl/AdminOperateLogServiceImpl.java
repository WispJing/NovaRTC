package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.AdminOperateLog;
import io.github.xiaotang.novartc.server.mapper.AdminOperateLogMapper;
import io.github.xiaotang.novartc.server.service.IAdminOperateLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 后台操作日志服务实现，提供后台操作日志的基础数据操作能力。
 */
@Service
public class AdminOperateLogServiceImpl extends ServiceImpl<AdminOperateLogMapper, AdminOperateLog> implements IAdminOperateLogService {

}
