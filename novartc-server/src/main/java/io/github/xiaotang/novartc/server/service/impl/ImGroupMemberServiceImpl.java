package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.mapper.ImGroupMemberMapper;
import io.github.xiaotang.novartc.server.service.IImGroupMemberService;
import io.github.xiaotang.novartc.server.service.UserRelationCacheService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 群成员服务实现，负责群成员关系维护并同步刷新关系缓存。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImGroupMemberServiceImpl extends ServiceImpl<ImGroupMemberMapper, ImGroupMember> implements IImGroupMemberService {
    private final UserRelationCacheService userRelationCacheService;

    /**
     * 新增群成员关系并刷新相关缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ImGroupMember entity) {
        boolean saved = super.save(entity);
        if (saved) {
            refreshRelationCaches(List.of(entity));
        }
        return saved;
    }

    /**
     * 批量新增群成员关系并刷新相关缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<ImGroupMember> entityList) {
        boolean saved = super.saveBatch(entityList);
        if (saved) {
            refreshRelationCaches(entityList);
        }
        return saved;
    }

    /**
     * 更新群成员关系并刷新相关缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(ImGroupMember entity) {
        ImGroupMember before = entity == null || StringUtils.isBlank(entity.getId()) ? null : getById(entity.getId());
        boolean updated = super.updateById(entity);
        if (updated) {
            Set<ImGroupMember> affected = new LinkedHashSet<>();
            if (before != null) {
                affected.add(before);
            }
            if (entity != null) {
                affected.add(entity);
            }
            refreshRelationCaches(affected);
        }
        return updated;
    }

    /**
     * 删除单个群成员关系并刷新相关缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(java.io.Serializable id) {
        ImGroupMember before = id == null ? null : getById(id);
        boolean removed = super.removeById(id);
        if (removed && before != null) {
            refreshRelationCaches(List.of(before));
        }
        return removed;
    }

    /**
     * 批量删除群成员关系并刷新相关缓存。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        List<java.io.Serializable> ids = list == null ? List.of() : list.stream()
                .filter(java.io.Serializable.class::isInstance)
                .map(java.io.Serializable.class::cast)
                .toList();
        List<ImGroupMember> before = ids.isEmpty() ? List.of() : listByIds(ids);
        boolean removed = super.removeByIds(list);
        if (removed) {
            refreshRelationCaches(before);
        }
        return removed;
    }

    private void refreshRelationCaches(Collection<ImGroupMember> members) {
        if (members == null || members.isEmpty()) {
            return;
        }

        Set<String> userIds = new LinkedHashSet<>();
        Set<String> groupIds = new LinkedHashSet<>();
        for (ImGroupMember member : members) {
            if (member == null) {
                continue;
            }
            if (StringUtils.isNotBlank(member.getUserId())) {
                userIds.add(member.getUserId());
            }
            if (StringUtils.isNotBlank(member.getGroupId())) {
                groupIds.add(member.getGroupId());
            }
        }

        userRelationCacheService.rebuildUserRelationsCachesAfterCommit(userIds);
        userRelationCacheService.rebuildGroupMembersCachesAfterCommit(groupIds);
    }
}
