package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.common.enums.ExceptionEnum;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.mapper.ImSingleChatMapper;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 单聊会话服务实现，负责单聊会话查询、复用与创建。
 */
@Service
@Transactional(readOnly = true)
public class ImSingleChatServiceImpl extends ServiceImpl<ImSingleChatMapper, ImSingleChat> implements IImSingleChatService {

    /**
     * 查询指定用户参与的单聊会话列表。
     */
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

    /**
     * 按双方用户标识查询已有单聊会话。
     */
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

    /**
     * 创建单聊会话，若已存在则返回现有会话。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImSingleChat createOrGet(String currentUserId, String targetUserId) {
        if (StringUtils.isAnyBlank(currentUserId, targetUserId)) {
            throw ex(ExceptionEnum.FRIEND_CHAT_USER_ID_REQUIRED);
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

    private ApplicationException ex(ExceptionEnum exceptionEnum) {
        return new ApplicationException(exceptionEnum.getValue(), exceptionEnum.getContent());
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
