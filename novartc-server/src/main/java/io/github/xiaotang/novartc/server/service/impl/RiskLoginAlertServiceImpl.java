package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.RiskLoginAlert;
import io.github.xiaotang.novartc.server.mapper.RiskLoginAlertMapper;
import io.github.xiaotang.novartc.server.service.IRiskLoginAlertService;
import org.springframework.stereotype.Service;

/**
 * 风控登录预警服务实现，提供登录预警记录的基础数据操作能力。
 */
@Service
public class RiskLoginAlertServiceImpl extends ServiceImpl<RiskLoginAlertMapper, RiskLoginAlert> implements IRiskLoginAlertService {
}
