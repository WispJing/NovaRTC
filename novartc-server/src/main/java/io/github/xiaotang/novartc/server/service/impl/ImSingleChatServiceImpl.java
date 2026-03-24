package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.mapper.ImSingleChatMapper;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

    @Override
    public ImSingleChat getByUsers(String user1Id, String user2Id) {
        if (StringUtils.isAnyBlank(user1Id, user2Id)) {
            return null;
        }

        var ordered = normalizeUserPair(user1Id, user2Id);
        ImSingleChat exact = getOne(new LambdaQueryWrapper<ImSingleChat>()
                .eq(ImSingleChat::getUser1Id, ordered[0])
                .eq(ImSingleChat::getUser2Id, ordered[1])
                .last("limit 1"));
        if (exact != null) {
            return exact;
        }

        return getOne(new LambdaQueryWrapper<ImSingleChat>()
                .eq(ImSingleChat::getUser1Id, ordered[1])
                .eq(ImSingleChat::getUser2Id, ordered[0])
                .last("limit 1"));
    }

    @Override
    public ImSingleChat createOrGet(String currentUserId, String targetUserId) {
        if (StringUtils.isAnyBlank(currentUserId, targetUserId)) {
            throw new ApplicationException("userId is blank");
        }
        if (StringUtils.equals(currentUserId, targetUserId)) {
            throw new ApplicationException("cannot chat with self");
        }

        ImSingleChat existing = getByUsers(currentUserId, targetUserId);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                existing.setIsDeleted(false);
                updateById(existing);
            }
            return existing;
        }

        var ordered = normalizeUserPair(currentUserId, targetUserId);
        ImSingleChat chat = new ImSingleChat();
        chat.setId(uuid32());
        chat.setChatId(uuid32());
        chat.setUser1Id(ordered[0]);
        chat.setUser2Id(ordered[1]);
        chat.setIsDeleted(false);
        save(chat);
        return chat;
    }

    private String[] normalizeUserPair(String left, String right) {
        return left.compareTo(right) <= 0
                ? new String[] { left, right }
                : new String[] { right, left };
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
