package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.RiskLoginAlert;
import io.github.xiaotang.novartc.server.mapper.RiskLoginAlertMapper;
import io.github.xiaotang.novartc.server.service.IRiskLoginAlertService;
import org.springframework.stereotype.Service;

@Service
public class RiskLoginAlertServiceImpl extends ServiceImpl<RiskLoginAlertMapper, RiskLoginAlert> implements IRiskLoginAlertService {
}
