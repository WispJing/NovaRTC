package io.github.xiaotang.novartc.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 解析在线状态事件的接收人，只通知相关好友和共同群成员。
 */
@Component
@RequiredArgsConstructor
public class PresenceAudienceResolver {
    private final IImSingleChatService imSingleChatService;
    private final IImGroupMemberService imGroupMemberService;

    public Set<String> resolveAudienceUserIds(String userId) {
        if (StringUtils.isBlank(userId)) {
            return Set.of();
        }
        Set<String> audienceUserIds = new LinkedHashSet<>();

        List<ImSingleChat> singleChats = imSingleChatService.getByUserId(userId);
        for (ImSingleChat singleChat : singleChats) {
            String peerUserId = userId.equals(singleChat.getUser1Id()) ? singleChat.getUser2Id() : singleChat.getUser1Id();
            if (StringUtils.isNotBlank(peerUserId) && !userId.equals(peerUserId)) {
                audienceUserIds.add(peerUserId);
            }
        }

        List<ImGroupMember> selfMemberships = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>()
                .eq(ImGroupMember::getUserId, userId)
                .eq(ImGroupMember::getIsDeleted, false));
        List<String> groupIds = selfMemberships.stream()
                .map(ImGroupMember::getGroupId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        if (!groupIds.isEmpty()) {
            List<ImGroupMember> groupMembers = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>()
                    .in(ImGroupMember::getGroupId, groupIds)
                    .eq(ImGroupMember::getIsDeleted, false));
            groupMembers.stream()
                    .map(ImGroupMember::getUserId)
                    .filter(StringUtils::isNotBlank)
                    .filter(memberUserId -> !userId.equals(memberUserId))
                    .forEach(audienceUserIds::add);
        }

        return audienceUserIds;
    }
}
