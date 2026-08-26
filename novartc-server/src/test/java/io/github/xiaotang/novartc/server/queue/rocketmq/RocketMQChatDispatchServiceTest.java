package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import io.github.xiaotang.novartc.server.netty.LocalSessionDispatcher;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.RocketMQHeader;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * RocketMQ 聊天消息分发服务测试，重点覆盖按实例对目标用户分组的逻辑：
 * 正常分组、无路由兜底、过期本地路由兜底以及本地实例剔除。
 */
@ExtendWith(MockitoExtension.class)
class RocketMQChatDispatchServiceTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private UserRouteService userRouteService;

    @Mock
    private LocalSessionDispatcher localSessionDispatcher;

    @Mock
    private InstanceIdProvider instanceIdProvider;

    @Mock
    private IMConfigProperties iMConfigProperties;

    @InjectMocks
    private RocketMQChatDispatchService dispatchService;

    /**
     * 有在线路由的用户按远程实例分组；多实例用户在每个所属实例分组中出现，
     * 结果中不应包含兜底分组。
     */
    @Test
    void shouldGroupRemoteUsersByInstance() {
        when(instanceIdProvider.getInstanceId()).thenReturn("inst-0");
        when(userRouteService.getUserInstances("user-a")).thenReturn(Set.of("inst-1"));
        when(userRouteService.getUserInstances("user-b")).thenReturn(Set.of("inst-1", "inst-2"));

        Map<String, Set<String>> grouped = dispatchService.groupTargetsByInstance(Set.of("user-a", "user-b"));

        assertEquals(Set.of("user-a", "user-b"), grouped.get("inst-1"));
        assertEquals(Set.of("user-b"), grouped.get("inst-2"));
        assertFalse(grouped.containsKey(RocketMQHeader.FALLBACK_TAG));
    }

    /**
     * 查不到任何在线路由的用户统一进入兜底分组，交由后续流程处理。
     */
    @Test
    void shouldRouteUsersWithoutRouteToFallback() {
        when(instanceIdProvider.getInstanceId()).thenReturn("inst-0");
        when(userRouteService.getUserInstances("user-c")).thenReturn(Set.of());

        Map<String, Set<String>> grouped = dispatchService.groupTargetsByInstance(Set.of("user-c"));

        assertEquals(Set.of("user-c"), grouped.get(RocketMQHeader.FALLBACK_TAG));
        assertEquals(1, grouped.size());
    }

    /**
     * 路由仅指向本实例（可能为残留的过期路由）的用户进入兜底分组，
     * 避免错误地把消息发回给自己。
     */
    @Test
    void shouldRouteStaleLocalOnlyRouteToFallback() {
        when(instanceIdProvider.getInstanceId()).thenReturn("inst-0");
        when(userRouteService.getUserInstances("user-d")).thenReturn(Set.of("inst-0"));

        Map<String, Set<String>> grouped = dispatchService.groupTargetsByInstance(Set.of("user-d"));

        assertEquals(Set.of("user-d"), grouped.get(RocketMQHeader.FALLBACK_TAG));
        assertEquals(1, grouped.size());
    }

    /**
     * 本实例 ID 不应出现在分组结果中，本地会话走本地分发而不经过 MQ。
     */
    @Test
    void shouldExcludeLocalInstanceFromGroups() {
        when(instanceIdProvider.getInstanceId()).thenReturn("inst-0");
        when(userRouteService.getUserInstances("user-e")).thenReturn(Set.of("inst-0", "inst-2"));

        Map<String, Set<String>> grouped = dispatchService.groupTargetsByInstance(Set.of("user-e"));

        assertEquals(Set.of("user-e"), grouped.get("inst-2"));
        assertFalse(grouped.containsKey("inst-0"));
    }
}
