package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.RiskGroupMute;
import io.github.xiaotang.novartc.server.mapper.RiskGroupMuteMapper;
import io.github.xiaotang.novartc.server.service.IRiskGroupMuteService;
import org.springframework.stereotype.Service;

/**
 * 风控群禁言服务实现，提供群禁言记录的基础数据操作能力。
 */
@Service
public class RiskGroupMuteServiceImpl extends ServiceImpl<RiskGroupMuteMapper, RiskGroupMute> implements IRiskGroupMuteService {
}
