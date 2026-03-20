package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.mapper.ImSingleChatMapper;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 单聊信息表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
@Service
public class ImSingleChatServiceImpl extends ServiceImpl<ImSingleChatMapper, ImSingleChat> implements IImSingleChatService {

    @Override
    public List<ImSingleChat> getByUserId(String userId) {
        return list(new LambdaQueryWrapper<ImSingleChat>()
                .eq(ImSingleChat::getIsDeleted, false)
                .and(
                        w -> w.eq(ImSingleChat::getUser1Id, userId)
                                .or()
                                .eq(ImSingleChat::getUser2Id, userId))
        );
    }
}
