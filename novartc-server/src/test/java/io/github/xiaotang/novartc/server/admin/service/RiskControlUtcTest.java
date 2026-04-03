package io.github.xiaotang.novartc.server.admin.service;

import io.github.xiaotang.novartc.server.admin.dto.AdminRiskAlertHandleRequest;
import io.github.xiaotang.novartc.server.entity.mysql.RiskLoginAlert;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.service.IRiskBlacklistService;
import io.github.xiaotang.novartc.server.service.IRiskGroupMuteService;
import io.github.xiaotang.novartc.server.service.IRiskLoginAlertService;
import io.github.xiaotang.novartc.server.service.IRiskUserControlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskControlUtcTest {

    @Mock
    private IRiskUserControlService riskUserControlService;
    @Mock
    private IRiskGroupMuteService riskGroupMuteService;
    @Mock
    private IRiskBlacklistService riskBlacklistService;
    @Mock
    private IRiskLoginAlertService riskLoginAlertService;
    @Mock
    private IImUserService imUserService;
    @Mock
    private IImGroupChatService imGroupChatService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RiskControlCacheService riskControlCacheService;
    @Mock
    private RiskControlEnforcementService riskControlEnforcementService;

    @InjectMocks
    private RiskControlService riskControlService;

    @Test
    void shouldStoreHandledAtAsInstant() {
        RiskLoginAlert entity = new RiskLoginAlert();
        entity.setId("alert-1");

        when(riskLoginAlertService.getById("alert-1")).thenReturn(entity);

        riskControlService.handleLoginAlert("alert-1", "admin-1", "Admin", new AdminRiskAlertHandleRequest());

        verify(riskLoginAlertService).updateById(any(RiskLoginAlert.class));
        assertNotNull(entity.getHandledAt());
        assertTrue(!entity.getHandledAt().isAfter(Instant.now()));
    }
}
