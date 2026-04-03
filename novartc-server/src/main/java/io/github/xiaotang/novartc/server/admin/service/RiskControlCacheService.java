package io.github.xiaotang.novartc.server.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.xiaotang.novartc.common.constant.LoginConstant;
import io.github.xiaotang.novartc.common.constant.RiskConstant;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.entity.mysql.RiskBlacklist;
import io.github.xiaotang.novartc.server.entity.mysql.RiskGroupMute;
import io.github.xiaotang.novartc.server.entity.mysql.RiskUserControl;
import io.github.xiaotang.novartc.server.service.IRiskBlacklistService;
import io.github.xiaotang.novartc.server.service.IRiskGroupMuteService;
import io.github.xiaotang.novartc.server.service.IRiskUserControlService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskControlCacheService {
    private static final Logger logger = LoggerFactory.getLogger(RiskControlCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final IRiskUserControlService riskUserControlService;
    private final IRiskGroupMuteService riskGroupMuteService;
    private final IRiskBlacklistService riskBlacklistService;

    public void warmupAllCaches() {
        clearAllRiskCaches();

        List<RiskUserControl> userControls = listActiveUserControls(null);
        Set<String> userIds = userControls.stream()
                .map(RiskUserControl::getUserId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        userIds.forEach(this::refreshUserControlCache);

        List<RiskGroupMute> groupMutes = listActiveGroupMutes(null);
        Set<String> groupIds = groupMutes.stream()
                .map(RiskGroupMute::getGroupId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        groupIds.forEach(this::refreshGroupMuteCache);

        List<RiskBlacklist> blacklists = listActiveBlacklists(null);
        Set<String> ownerUserIds = blacklists.stream()
                .map(RiskBlacklist::getOwnerUserId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        ownerUserIds.forEach(this::refreshBlacklistCache);

        logger.info("Risk caches warmed up: userControls={}, groupMutes={}, blacklists={}", userControls.size(), groupMutes.size(), blacklists.size());
    }

    public void refreshUserControlCache(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        List<RiskUserControl> items = listActiveUserControls(userId);
        writeJson(buildUserControlKey(userId), items);
    }

    public void refreshGroupMuteCache(String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return;
        }
        List<RiskGroupMute> items = listActiveGroupMutes(groupId);
        clearGroupMuteCache(groupId);

        List<RiskGroupMute> groupAllMutes = items.stream()
                .filter(item -> StringUtils.equals(item.getMuteScope(), RiskConstant.GROUP_MUTE_SCOPE_ALL))
                .toList();
        writeJson(buildGroupAllMuteKey(groupId), groupAllMutes);

        Map<String, List<RiskGroupMute>> userMuteMap = items.stream()
                .filter(item -> StringUtils.equals(item.getMuteScope(), RiskConstant.GROUP_MUTE_SCOPE_USER) && StringUtils.isNotBlank(item.getUserId()))
                .collect(Collectors.groupingBy(RiskGroupMute::getUserId, LinkedHashMap::new, Collectors.toCollection(ArrayList::new)));

        userMuteMap.forEach((userId, userMutes) -> {
            String key = buildGroupUserMuteKey(groupId, userId);
            redisTemplate.opsForSet().add(buildGroupMuteIndexKey(groupId), key);
            writeJson(key, userMutes);
        });
    }

    public void refreshBlacklistCache(String ownerUserId) {
        if (StringUtils.isBlank(ownerUserId)) {
            return;
        }
        List<RiskBlacklist> items = listActiveBlacklists(ownerUserId);
        replaceSet(buildBlacklistKey(ownerUserId), items.stream()
                .map(RiskBlacklist::getBlockedUserId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public RiskUserControl findActiveUserControl(String userId, String controlType, String scope) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        List<String> scopes = LoginConstant.LOGIN_SCOPE_ALL.equals(scope) ? List.of(LoginConstant.LOGIN_SCOPE_ALL) : List.of(scope, LoginConstant.LOGIN_SCOPE_ALL);
        return loadJsonList(buildUserControlKey(userId), new TypeReference<List<RiskUserControl>>() {
                }, () -> refreshUserControlCache(userId))
                .stream()
                .filter(item -> StringUtils.equals(item.getControlType(), controlType))
                .filter(item -> scopes.contains(item.getScope()))
                .filter(this::isActive)
                .findFirst()
                .orElse(null);
    }

    public RiskGroupMute findActiveGroupMute(String groupId, String userId, String muteScope) {
        if (StringUtils.isBlank(groupId) || StringUtils.isBlank(muteScope)) {
            return null;
        }
        if (StringUtils.equals(muteScope, RiskConstant.GROUP_MUTE_SCOPE_ALL)) {
            return loadJsonList(buildGroupAllMuteKey(groupId), new TypeReference<List<RiskGroupMute>>() {
                    }, () -> refreshGroupMuteCache(groupId))
                    .stream()
                    .filter(this::isActive)
                    .findFirst()
                    .orElse(null);
        }
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return loadJsonList(buildGroupUserMuteKey(groupId, userId), new TypeReference<List<RiskGroupMute>>() {
                }, () -> refreshGroupMuteCache(groupId))
                .stream()
                .filter(this::isActive)
                .findFirst()
                .orElse(null);
    }

    public boolean isBlacklisted(String ownerUserId, String blockedUserId) {
        if (StringUtils.isAnyBlank(ownerUserId, blockedUserId)) {
            return false;
        }
        return loadSet(buildBlacklistKey(ownerUserId), () -> refreshBlacklistCache(ownerUserId)).contains(blockedUserId)
                || loadSet(buildBlacklistKey(blockedUserId), () -> refreshBlacklistCache(blockedUserId)).contains(ownerUserId);
    }

    private List<RiskUserControl> listActiveUserControls(String userId) {
        return riskUserControlService.list(new LambdaQueryWrapper<RiskUserControl>()
                        .eq(StringUtils.isNotBlank(userId), RiskUserControl::getUserId, userId)
                        .eq(RiskUserControl::getStatus, (byte) 1)
                        .eq(RiskUserControl::getIsDeleted, false)
                        .orderByDesc(RiskUserControl::getCreatedAt))
                .stream()
                .filter(this::isActive)
                .toList();
    }

    private List<RiskGroupMute> listActiveGroupMutes(String groupId) {
        return riskGroupMuteService.list(new LambdaQueryWrapper<RiskGroupMute>()
                        .eq(StringUtils.isNotBlank(groupId), RiskGroupMute::getGroupId, groupId)
                        .eq(RiskGroupMute::getStatus, (byte) 1)
                        .eq(RiskGroupMute::getIsDeleted, false)
                        .orderByDesc(RiskGroupMute::getCreatedAt))
                .stream()
                .filter(this::isActive)
                .toList();
    }

    private List<RiskBlacklist> listActiveBlacklists(String ownerUserId) {
        return riskBlacklistService.list(new LambdaQueryWrapper<RiskBlacklist>()
                .eq(StringUtils.isNotBlank(ownerUserId), RiskBlacklist::getOwnerUserId, ownerUserId)
                .eq(RiskBlacklist::getStatus, (byte) 1)
                .eq(RiskBlacklist::getIsDeleted, false)
                .orderByDesc(RiskBlacklist::getCreatedAt));
    }

    private boolean isActive(RiskUserControl item) {
        return item != null && Byte.valueOf((byte) 1).equals(item.getStatus()) && (item.getExpiresAt() == null || item.getExpiresAt().isAfter(Instant.now()));
    }

    private boolean isActive(RiskGroupMute item) {
        return item != null && Byte.valueOf((byte) 1).equals(item.getStatus()) && (item.getExpiresAt() == null || item.getExpiresAt().isAfter(Instant.now()));
    }

    private <T> List<T> loadJsonList(String key, TypeReference<List<T>> typeReference, Runnable reloader) {
        String loadedKey = buildLoadedKey(key);
        String json = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)) {
            return JacksonUtil.toObject(json, typeReference);
        }
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(loadedKey))) {
            reloader.run();
            json = redisTemplate.opsForValue().get(key);
        }
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        return JacksonUtil.toObject(json, typeReference);
    }

    private Set<String> loadSet(String key, Runnable reloader) {
        Set<String> values = readSet(key);
        if (!values.isEmpty() || Boolean.TRUE.equals(redisTemplate.hasKey(buildLoadedKey(key)))) {
            return values;
        }
        reloader.run();
        return readSet(key);
    }

    private Set<String> readSet(String key) {
        Set<String> values = redisTemplate.opsForSet().members(key);
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void writeJson(String key, Object value) {
        String loadedKey = buildLoadedKey(key);
        redisTemplate.opsForValue().set(loadedKey, "1");
        if (value instanceof Collection<?> collection && collection.isEmpty()) {
            redisTemplate.delete(key);
            return;
        }
        redisTemplate.opsForValue().set(key, JacksonUtil.toJson(value));
    }

    private void replaceSet(String key, Collection<String> values) {
        redisTemplate.delete(key);
        redisTemplate.opsForValue().set(buildLoadedKey(key), "1");
        if (values == null || values.isEmpty()) {
            return;
        }
        redisTemplate.opsForSet().add(key, values.stream().filter(StringUtils::isNotBlank).distinct().toArray(String[]::new));
    }

    private void clearGroupMuteCache(String groupId) {
        Set<String> keys = new LinkedHashSet<>();
        String indexKey = buildGroupMuteIndexKey(groupId);
        Set<String> indexedKeys = redisTemplate.opsForSet().members(indexKey);
        if (indexedKeys != null) {
            keys.addAll(indexedKeys);
            indexedKeys.forEach(item -> keys.add(buildLoadedKey(item)));
        }
        keys.add(indexKey);
        keys.add(buildLoadedKey(indexKey));
        keys.add(buildGroupAllMuteKey(groupId));
        keys.add(buildLoadedKey(buildGroupAllMuteKey(groupId)));
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void clearAllRiskCaches() {
        Set<String> keys = redisTemplate.keys(RiskConstant.REDIS_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String buildUserControlKey(String userId) {
        return RiskConstant.USER_CONTROL_PREFIX + userId;
    }

    private String buildGroupAllMuteKey(String groupId) {
        return RiskConstant.GROUP_ALL_MUTE_PREFIX + groupId;
    }

    private String buildGroupUserMuteKey(String groupId, String userId) {
        return RiskConstant.GROUP_USER_MUTE_PREFIX + groupId + ":" + userId;
    }

    private String buildGroupMuteIndexKey(String groupId) {
        return RiskConstant.GROUP_USER_MUTE_INDEX_PREFIX + groupId;
    }

    private String buildBlacklistKey(String ownerUserId) {
        return RiskConstant.BLACKLIST_PREFIX + ownerUserId;
    }

    private String buildLoadedKey(String key) {
        return key + ":loaded";
    }
}


