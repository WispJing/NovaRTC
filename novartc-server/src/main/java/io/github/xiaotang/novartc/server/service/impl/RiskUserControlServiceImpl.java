package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.RiskUserControl;
import io.github.xiaotang.novartc.server.mapper.RiskUserControlMapper;
import io.github.xiaotang.novartc.server.service.IRiskUserControlService;
import org.springframework.stereotype.Service;

/**
 * 风控用户管控服务实现，提供用户管控记录的基础数据操作能力。
 */
@Service
public class RiskUserControlServiceImpl extends ServiceImpl<RiskUserControlMapper, RiskUserControl> implements IRiskUserControlService {
}
