package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.mapper.ImGroupChatMapper;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 群聊服务实现，负责群会话信息查询与基础数据操作。
 */
@Service
public class ImGroupChatServiceImpl extends ServiceImpl<ImGroupChatMapper, ImGroupChat> implements IImGroupChatService {

    /**
     * 查询指定用户参与的群聊列表。
     */
    @Override
    public List<ImGroupChat> getByUserId(String userId) {
        return baseMapper.selectGroupChatsByUserId(userId);
    }
}
