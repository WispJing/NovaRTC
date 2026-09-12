package io.github.xiaotang.novartc.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriend;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.mapper.ImFriendMapper;
import io.github.xiaotang.novartc.server.mapper.ImGroupMemberMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户关系缓存服务，负责好友关系和群成员缓存的重建与查询。
 */
@Service
@RequiredArgsConstructor
public class UserRelationCacheService {
    /**
     * 原子替换关系集合并写入已加载标记，避免并发读取看到中间状态。
     */
    private static final DefaultRedisScript<Long> REPLACE_SET_SCRIPT = new DefaultRedisScript<>("""
            redis.call('DEL', KEYS[1])
            local chunkSize = 1000
            for startIndex = 1, #ARGV, chunkSize do
                local chunk = {}
                local endIndex = math.min(startIndex + chunkSize - 1, #ARGV)
                for index = startIndex, endIndex do
                    chunk[#chunk + 1] = ARGV[index]
                end
                redis.call('SADD', KEYS[1], unpack(chunk))
            end
            redis.call('SET', KEYS[2], '1')
            return 1
            """, Long.class);

    /**
     * 条带锁，防止缓存击穿：并发未命中时同一条带内只允许一次重建。
     */
    private static final Object[] REBUILD_LOCKS = new Object[64];

    static {
        for (int i = 0; i < REBUILD_LOCKS.length; i++) {
            REBUILD_LOCKS[i] = new Object();
        }
    }

    private final StringRedisTemplate redisTemplate;
    private final IMConfigProperties imConfigProperties;
    private final ImFriendMapper imFriendMapper;
    private final ImGroupMemberMapper imGroupMemberMapper;

    /**
     * 缓存重建线程池，事务提交后的重建任务显式提交到该线程池，避免 @Async 自调用失效。
     */
    @Qualifier("cacheRebuildExecutor")
    private final ThreadPoolTaskExecutor cacheRebuildExecutor;

    /**
     * 异步重建指定用户的关系缓存。
     */
    @Async("cacheRebuildExecutor")
    public void rebuildUserRelationsCacheAsync(String userId) {
        rebuildUserRelationsCache(userId);
    }

    /**
     * 异步批量重建多个用户的关系缓存。
     */
    @Async("cacheRebuildExecutor")
    public void rebuildUserRelationsCachesAsync(Collection<String> userIds) {
        if (userIds == null) {
            return;
        }
        userIds.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(this::rebuildUserRelationsCache);
    }

    /**
     * 异步重建指定群的成员缓存。
     */
    @Async("cacheRebuildExecutor")
    public void rebuildGroupMembersCacheAsync(String groupId) {
        rebuildGroupMembersCache(groupId);
    }

    /**
     * 异步批量重建多个群的成员缓存。
     */
    @Async("cacheRebuildExecutor")
    public void rebuildGroupMembersCachesAsync(Collection<String> groupIds) {
        if (groupIds == null) {
            return;
        }
        groupIds.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .forEach(this::rebuildGroupMembersCache);
    }

    /**
     * 同步重建指定用户的关系缓存。
     */
    public void rebuildUserRelationsCache(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }

        List<String> friendIds = imFriendMapper.selectList(new LambdaQueryWrapper<ImFriend>()
                        .eq(ImFriend::getIsDeleted, false)
                        .eq(ImFriend::getUserId, userId))
                .stream()
                .map(ImFriend::getFriendId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        replaceSet(buildUserFriendKey(userId), friendIds);

        List<String> groupIds = imGroupMemberMapper.selectList(new LambdaQueryWrapper<ImGroupMember>()
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

    /**
     * 在事务提交后异步重建多个用户的关系缓存；无活动事务时立即异步重建。
     *
     * <p>好友关系写入（事务内）与缓存重建并发时，重建可能读到未提交数据，
     * 导致缓存永久滞后、新好友互发消息被拦截，关系变更场景必须使用本方法。</p>
     */
    public void rebuildUserRelationsCachesAfterCommit(Collection<String> userIds) {
        List<String> targets = normalizeIds(userIds);
        if (targets.isEmpty()) {
            return;
        }
        runAfterCommit(() -> targets.forEach(this::rebuildUserRelationsCache));
    }

    /**
     * 在事务提交后异步重建多个群的成员缓存；无活动事务时立即异步重建。
     */
    public void rebuildGroupMembersCachesAfterCommit(Collection<String> groupIds) {
        List<String> targets = normalizeIds(groupIds);
        if (targets.isEmpty()) {
            return;
        }
        runAfterCommit(() -> targets.forEach(this::rebuildGroupMembersCache));
    }

    /**
     * 事务提交后提交重建任务到缓存重建线程池；无活动事务时立即提交。
     */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheRebuildExecutor.execute(task);
                }
            });
        } else {
            cacheRebuildExecutor.execute(task);
        }
    }

    private List<String> normalizeIds(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    /**
     * 同步重建指定群的成员缓存。
     */
    public void rebuildGroupMembersCache(String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return;
        }

        List<String> memberUserIds = imGroupMemberMapper.selectList(new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getIsDeleted, false)
                        .eq(ImGroupMember::getGroupId, groupId))
                .stream()
                .map(ImGroupMember::getUserId)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        replaceSet(buildGroupMembersKey(groupId), memberUserIds);
    }

    /**
     * 判断两个用户是否为好友。
     */
    public boolean isFriend(String userId, String targetUserId) {
        if (StringUtils.isAnyBlank(userId, targetUserId)) {
            return false;
        }
        if (StringUtils.equals(userId, targetUserId)) {
            return true;
        }
        if (getFriendUserIds(userId).contains(targetUserId)) {
            return true;
        }
        // 缓存可能滞后（如关系变更事务提交时机竞争），以数据库为准兜底，命中后触发自愈重建
        Long friendCount = imFriendMapper.selectCount(new LambdaQueryWrapper<ImFriend>()
                .eq(ImFriend::getIsDeleted, false)
                .eq(ImFriend::getUserId, userId)
                .eq(ImFriend::getFriendId, targetUserId));
        if (friendCount == null || friendCount == 0) {
            return false;
        }
        cacheRebuildExecutor.execute(() -> rebuildUserRelationsCache(userId));
        return true;
    }

    /**
     * 获取指定用户的好友用户 ID 集合。
     */
    public Set<String> getFriendUserIds(String userId) {
        return loadSet(buildUserFriendKey(userId), () -> {
            rebuildUserRelationsCache(userId);
            return readSet(buildUserFriendKey(userId));
        });
    }

    /**
     * 获取指定用户加入的群组 ID 集合。
     */
    public Set<String> getUserGroupIds(String userId) {
        return loadSet(buildUserGroupKey(userId), () -> {
            rebuildUserRelationsCache(userId);
            return readSet(buildUserGroupKey(userId));
        });
    }

    /**
     * 获取指定群的成员用户 ID 集合。
     */
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
        // 缓存未命中：条带锁内双检后重建，防止并发击穿导致重复查库
        synchronized (REBUILD_LOCKS[Math.floorMod(key.hashCode(), REBUILD_LOCKS.length)]) {
            cached = readSet(key);
            if (!cached.isEmpty() || Boolean.TRUE.equals(redisTemplate.hasKey(buildLoadedKey(key)))) {
                return cached;
            }
            return reloader.get();
        }
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
        String[] members = values == null ? new String[0] : values.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toArray(String[]::new);
        redisTemplate.execute(REPLACE_SET_SCRIPT, List.of(key, buildLoadedKey(key)), (Object[]) members);
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
