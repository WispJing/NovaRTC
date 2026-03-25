package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 解析在线状态事件的接收人，只通知相关好友和共同群成员。
 */
@Component
@RequiredArgsConstructor
public class PresenceAudienceResolver {
    private final UserRelationCacheService userRelationCacheService;

    public Set<String> resolveAudienceUserIds(String userId) {
        if (StringUtils.isBlank(userId)) {
            return Set.of();
        }
        Set<String> audienceUserIds = new LinkedHashSet<>(userRelationCacheService.getFriendUserIds(userId));

        for (String groupId : userRelationCacheService.getUserGroupIds(userId)) {
            for (String memberUserId : userRelationCacheService.getGroupMemberUserIds(groupId)) {
                if (StringUtils.isNotBlank(memberUserId) && !StringUtils.equals(userId, memberUserId)) {
                    audienceUserIds.add(memberUserId);
                }
            }
        }

        return audienceUserIds;
    }
}

