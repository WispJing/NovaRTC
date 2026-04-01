package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.RiskBlacklist;
import io.github.xiaotang.novartc.server.mapper.RiskBlacklistMapper;
import io.github.xiaotang.novartc.server.service.IRiskBlacklistService;
import org.springframework.stereotype.Service;

/**
 * 风控黑名单服务实现，提供黑名单记录的基础数据操作能力。
 */
@Service
public class RiskBlacklistServiceImpl extends ServiceImpl<RiskBlacklistMapper, RiskBlacklist> implements IRiskBlacklistService {
}
