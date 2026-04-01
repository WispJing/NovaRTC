package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.RiskGroupMute;
import io.github.xiaotang.novartc.server.mapper.RiskGroupMuteMapper;
import io.github.xiaotang.novartc.server.service.IRiskGroupMuteService;
import org.springframework.stereotype.Service;

@Service
public class RiskGroupMuteServiceImpl extends ServiceImpl<RiskGroupMuteMapper, RiskGroupMute> implements IRiskGroupMuteService {
}
