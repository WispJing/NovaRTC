package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created on 2026/3/20 11:35
 *
 * @author tangj
 * @description
 */
@RequiredArgsConstructor
@Service
public class ChatServiceImpl implements ChatService {
    private final IImUserService imUserService;
    private final IImSingleChatService imSingleChatService;
    private final IImGroupChatService imGroupChatService;
    private final IImGroupMemberService imGroupMemberService;

    @Override
    public List<ChatInfoVO> chatList() {
        String currentUserId = UserContextHolder.getUserId();

        // 1. 查单聊
        List<ImSingleChat> singleChats = imSingleChatService.list(
                new LambdaQueryWrapper<ImSingleChat>()
                        .eq(ImSingleChat::getIsDeleted, false)
                        .and(w -> w.eq(ImSingleChat::getUser1Id, currentUserId)
                                .or()
                                .eq(ImSingleChat::getUser2Id, currentUserId))
        );

        // 2. 查单聊对方用户ID
        Set<String> targetUserIds = singleChats.stream()
                .map(chat -> chat.getUser1Id().equals(currentUserId) ? chat.getUser2Id() : chat.getUser1Id())
                .collect(Collectors.toSet());

        // 3. 批量查用户信息，避免 N+1
        Map<String, ImUser> userMap = imUserService.list(
                new LambdaQueryWrapper<ImUser>()
                        .in(ImUser::getId, targetUserIds)
                        .eq(ImUser::getIsDeleted, false)
        ).stream().collect(Collectors.toMap(ImUser::getId, Function.identity()));

        // 4. 组装单聊 VO
        List<ChatInfoVO> singleVos = singleChats.stream().map(chat -> {
            String targetUserId = chat.getUser1Id().equals(currentUserId)
                    ? chat.getUser2Id()
                    : chat.getUser1Id();

            ImUser targetUser = userMap.get(targetUserId);

            ChatInfoVO vo = new ChatInfoVO();
            vo.setChatType("single");
            vo.setChatId(chat.getChatId());
            vo.setTargetUserId(targetUserId);

            if (targetUser != null) {
                vo.setTitle(targetUser.getUsername());
                vo.setTargetUserNum(targetUser.getUserNum());
                vo.setAvatarUrl(targetUser.getAvatarUrl());
            } else {
                vo.setTitle("未知用户");
            }

            return vo;
        }).toList();

        List<ChatInfoVO> result = new ArrayList<>(singleVos);

        // 5. 查群关系
        List<ImGroupMember> members = imGroupMemberService.list(
                new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getUserId, currentUserId)
                        .eq(ImGroupMember::getIsDeleted, false)
        );

        List<String> groupIds = members.stream()
                .map(ImGroupMember::getGroupId)
                .toList();

        if (!groupIds.isEmpty()) {
            // 6. 查群聊
            List<ImGroupChat> groupChats = imGroupChatService.list(
                    new LambdaQueryWrapper<ImGroupChat>()
                            .in(ImGroupChat::getId, groupIds)
                            .eq(ImGroupChat::getIsDeleted, false)
            );

            // 7. 组装群聊 VO
            List<ChatInfoVO> groupVos = groupChats.stream().map(group -> {
                ChatInfoVO vo = new ChatInfoVO();
                vo.setChatType("group");
                vo.setChatId(group.getChatId());
                vo.setGroupId(group.getId());
                vo.setGroupNum(group.getGroupNum());
                vo.setTitle(group.getGroupName());
                return vo;
            }).toList();

            result.addAll(groupVos);
        }

        return result;
    }
}
