package io.github.xiaotang.novartc.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriend;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRelationCacheService {
    private final StringRedisTemplate redisTemplate;
    private final IMConfigProperties imConfigProperties;
    private final IImFriendService imFriendService;
    private final IImGroupMemberService imGroupMemberService;

    @Async
    public void rebuildUserRelationsCacheAsync(String userId) {
        rebuildUserRelationsCache(userId);
    }

    @Async
    public void rebuildUserRelationsCachesAsync(Collection<String> userIds) {
        if (userIds == null) {
            return;
        }
        userIds.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(this::rebuildUserRelationsCache);
    }

    @Async
    public void rebuildGroupMembersCacheAsync(String groupId) {
        rebuildGroupMembersCache(groupId);
    }

    @Async
    public void rebuildGroupMembersCachesAsync(Collection<String> groupIds) {
        if (groupIds == null) {
            return;
        }
        groupIds.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(this::rebuildGroupMembersCache);
    }

    public void rebuildUserRelationsCache(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }

        List<String> friendIds = imFriendService.list(new LambdaQueryWrapper<ImFriend>()
                        .eq(ImFriend::getIsDeleted, false)
                        .eq(ImFriend::getUserId, userId))
                .stream()
                .map(ImFriend::getFriendId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        replaceSet(buildUserFriendKey(userId), friendIds);

        List<String> groupIds = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getIsDeleted, false)
                        .eq(ImGroupMember::getUserId, userId))
                .stream()
                .map(ImGroupMember::getGroupId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        replaceSet(buildUserGroupKey(userId), groupIds);

        groupIds.forEach(this::rebuildGroupMembersCache);
    }

    public void rebuildGroupMembersCache(String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return;
        }

        List<String> memberUserIds = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getIsDeleted, false)
                        .eq(ImGroupMember::getGroupId, groupId))
                .stream()
                .map(ImGroupMember::getUserId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        replaceSet(buildGroupMembersKey(groupId), memberUserIds);
    }

    public boolean isFriend(String userId, String targetUserId) {
        if (StringUtils.isAnyBlank(userId, targetUserId)) {
            return false;
        }
        if (StringUtils.equals(userId, targetUserId)) {
            return true;
        }
        return getFriendUserIds(userId).contains(targetUserId);
    }

    public Set<String> getFriendUserIds(String userId) {
        return loadSet(buildUserFriendKey(userId), () -> {
            rebuildUserRelationsCache(userId);
            return readSet(buildUserFriendKey(userId));
        });
    }

    public Set<String> getUserGroupIds(String userId) {
        return loadSet(buildUserGroupKey(userId), () -> {
            rebuildUserRelationsCache(userId);
            return readSet(buildUserGroupKey(userId));
        });
    }

    public Set<String> getGroupMemberUserIds(String groupId) {
        return loadSet(buildGroupMembersKey(groupId), () -> {
            rebuildGroupMembersCache(groupId);
            return readSet(buildGroupMembersKey(groupId));
        });
    }

    private Set<String> loadSet(String key, java.util.function.Supplier<Set<String>> reloader) {
        if (StringUtils.isBlank(key)) {
            return Set.of();
        }
        Set<String> cached = readSet(key);
        if (!cached.isEmpty() || Boolean.TRUE.equals(redisTemplate.hasKey(buildLoadedKey(key)))) {
            return cached;
        }
        return reloader.get();
    }

    private Set<String> readSet(String key) {
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members == null || members.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return members.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void replaceSet(String key, Collection<String> values) {
        redisTemplate.delete(key);
        redisTemplate.opsForValue().set(buildLoadedKey(key), "1");
        if (values == null || values.isEmpty()) {
            return;
        }
        redisTemplate.opsForSet().add(key, values.stream().filter(StringUtils::isNotBlank).distinct().toArray(String[]::new));
    }

    private String buildUserFriendKey(String userId) {
        return imConfigProperties.getUserFriendCacheKeyPreFix() + userId;
    }

    private String buildUserGroupKey(String userId) {
        return imConfigProperties.getUserGroupCacheKeyPreFix() + userId;
    }

    private String buildGroupMembersKey(String groupId) {
        return imConfigProperties.getGroupMembersCacheKeyPreFix() + groupId;
    }

    private String buildLoadedKey(String dataKey) {
        return dataKey + ":loaded";
    }
}
