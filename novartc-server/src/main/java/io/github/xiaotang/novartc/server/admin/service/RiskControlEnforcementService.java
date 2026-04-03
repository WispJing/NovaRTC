package io.github.xiaotang.novartc.server.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.constant.LoginConstant;
import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.entity.mysql.AdminLoginLog;
import io.github.xiaotang.novartc.server.queue.TokenKickDispatchService;
import io.github.xiaotang.novartc.server.service.IAdminLoginLogService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RiskControlEnforcementService {
    private final StringRedisTemplate redisTemplate;
    private final TokenKickDispatchService tokenKickDispatchService;
    private final IAdminLoginLogService adminLoginLogService;

    public void forceLogoutForLoginBan(String userId, String scope) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        String finalScope = StringUtils.defaultIfBlank(scope, "IM");
        if ("IM".equals(finalScope) || "ALL".equals(finalScope)) {
            forceLogoutImSessions(userId);
        }
        if ("ADMIN".equals(finalScope) || "ALL".equals(finalScope)) {
            forceLogoutAdminSessions(userId);
        }
    }

    public void forceBlockBlacklistSessions(String leftUserId, String rightUserId) {
        forceLogoutImSessions(leftUserId);
        forceLogoutImSessions(rightUserId);
    }

    private void forceLogoutImSessions(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        String userTokensKey = LoginConstant.USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens == null || tokens.isEmpty()) {
            redisTemplate.delete(userTokensKey);
            return;
        }
        Set<String> copied = new LinkedHashSet<>(tokens);
        for (String token : copied) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            redisTemplate.delete(LoginConstant.TOKEN_PREFIX + token);
            redisTemplate.opsForSet().remove(userTokensKey, token);
            tokenKickDispatchService.dispatch(new TokenKickMessage(userId, token));
        }
        redisTemplate.delete(userTokensKey);
    }

    private void forceLogoutAdminSessions(String userId) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        String userTokensKey = LoginConstant.ADMIN_USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens == null || tokens.isEmpty()) {
            redisTemplate.delete(userTokensKey);
            return;
        }
        Instant logoutTime = Instant.now();
        for (String token : new LinkedHashSet<>(tokens)) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            redisTemplate.delete(LoginConstant.ADMIN_TOKEN_PREFIX + token);
            redisTemplate.opsForSet().remove(userTokensKey, token);
            AdminLoginLog log = adminLoginLogService.getOne(new LambdaQueryWrapper<AdminLoginLog>()
                    .eq(AdminLoginLog::getToken, token)
                    .eq(AdminLoginLog::getIsDeleted, false)
                    .orderByDesc(AdminLoginLog::getCreatedAt)
                    .last("limit 1"));
            if (log != null) {
                log.setStatus((byte) 2);
                log.setFailReason("触发风控策略，强制下线");
                log.setLogoutTime(logoutTime);
                adminLoginLogService.updateById(log);
            }
        }
        redisTemplate.delete(userTokensKey);
    }
}
