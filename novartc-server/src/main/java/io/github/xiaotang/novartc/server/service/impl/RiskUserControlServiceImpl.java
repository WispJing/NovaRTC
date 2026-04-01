package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.RiskUserControl;
import io.github.xiaotang.novartc.server.mapper.RiskUserControlMapper;
import io.github.xiaotang.novartc.server.service.IRiskUserControlService;
import org.springframework.stereotype.Service;

@Service
public class RiskUserControlServiceImpl extends ServiceImpl<RiskUserControlMapper, RiskUserControl> implements IRiskUserControlService {
}
