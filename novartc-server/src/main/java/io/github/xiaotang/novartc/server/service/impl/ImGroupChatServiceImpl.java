package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.mapper.ImGroupChatMapper;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 群聊信息表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
@Service
public class ImGroupChatServiceImpl extends ServiceImpl<ImGroupChatMapper, ImGroupChat> implements IImGroupChatService {

    @Override
    public List<ImGroupChat> getByUserId(String userId) {
        return baseMapper.selectGroupChatsByUserId(userId);
    }
}
