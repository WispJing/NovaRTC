package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.mapper.ImFriendMapper;
import io.github.xiaotang.novartc.server.mapper.ImGroupMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 好友关系缓存服务测试，重点覆盖缓存滞后时以数据库兜底的自愈逻辑。
 */
@ExtendWith(MockitoExtension.class)
class UserRelationCacheServiceTest {

    private static final String FRIEND_KEY_PREFIX = "im:friend:";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private IMConfigProperties imConfigProperties;

    @Mock
    private ImFriendMapper imFriendMapper;

    @Mock
    private ImGroupMemberMapper imGroupMemberMapper;

    @Mock
    private ThreadPoolTaskExecutor cacheRebuildExecutor;

    @Mock
    private SetOperations<String, String> setOperations;

    private UserRelationCacheService userRelationCacheService;

    @BeforeEach
    void setUp() {
        userRelationCacheService = new UserRelationCacheService(
                redisTemplate, imConfigProperties, imFriendMapper, imGroupMemberMapper, cacheRebuildExecutor);
    }

    @Test
    void isFriendShouldHitCacheWithoutDbQuery() {
        when(imConfigProperties.getUserFriendCacheKeyPreFix()).thenReturn(FRIEND_KEY_PREFIX);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("im:friend:u1")).thenReturn(Set.of("f1"));

        assertTrue(userRelationCacheService.isFriend("u1", "f1"));
        verify(imFriendMapper, never()).selectCount(any());
        verify(cacheRebuildExecutor, never()).execute(any());
    }

    @Test
    void isFriendShouldFallbackToDbAndTriggerSelfHealingWhenCacheStale() {
        when(imConfigProperties.getUserFriendCacheKeyPreFix()).thenReturn(FRIEND_KEY_PREFIX);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("im:friend:u1")).thenReturn(Set.of());
        when(redisTemplate.hasKey("im:friend:u1:loaded")).thenReturn(true);
        when(imFriendMapper.selectCount(any())).thenReturn(1L);

        assertTrue(userRelationCacheService.isFriend("u1", "f1"));
        verify(cacheRebuildExecutor, times(1)).execute(any());
    }

    @Test
    void isFriendShouldReturnFalseWhenDbHasNoRelation() {
        when(imConfigProperties.getUserFriendCacheKeyPreFix()).thenReturn(FRIEND_KEY_PREFIX);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("im:friend:u1")).thenReturn(Set.of());
        when(redisTemplate.hasKey("im:friend:u1:loaded")).thenReturn(true);
        when(imFriendMapper.selectCount(any())).thenReturn(0L);

        assertFalse(userRelationCacheService.isFriend("u1", "f1"));
        verify(cacheRebuildExecutor, never()).execute(any());
    }
}
