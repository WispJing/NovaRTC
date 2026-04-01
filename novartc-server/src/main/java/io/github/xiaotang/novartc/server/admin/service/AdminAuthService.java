package io.github.xiaotang.novartc.server.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.constant.LoginConstant;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.common.util.JwtUtil;
import io.github.xiaotang.novartc.common.util.MD5Util;
import io.github.xiaotang.novartc.server.admin.dto.AdminLoginRequest;
import io.github.xiaotang.novartc.server.admin.vo.AdminLoginVO;
import io.github.xiaotang.novartc.server.entity.mysql.AdminLoginLog;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.service.IAdminLoginLogService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final StringRedisTemplate redisTemplate;
    private final IImUserService imUserService;
    private final IAdminLoginLogService adminLoginLogService;
    private final AdminPermissionService adminPermissionService;

    public AdminLoginVO login(AdminLoginRequest request, HttpServletRequest httpRequest) {
        if (request == null || StringUtils.isAnyBlank(request.getUserNum(), request.getPassword())) {
            saveLoginLog(null, null, null, request == null ? null : request.getClientType(), null, (byte) 0, "参数错误", httpRequest);
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        ImUser user = imUserService.selectByUserNum(request.getUserNum());
        if (user == null || !MD5Util.verify(request.getPassword(), user.getPassword())) {
            saveLoginLog(null, request.getUserNum(), null, request.getClientType(), null, (byte) 0, "用户名或密码错误", httpRequest);
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }
        if (!adminPermissionService.hasAdminAccess(user.getId())) {
            saveLoginLog(user, user.getUserNum(), user.getUsername(), request.getClientType(), null, (byte) 0, "无后台访问权限", httpRequest);
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        String clientType = StringUtils.defaultIfBlank(request.getClientType(), "admin-web");
        String token = JwtUtil.generateToken(user.getId(), Map.of("loginType", "ADMIN", "clientType", clientType));
        kickByClientType(user.getId(), clientType);

        String tokenKey = LoginConstant.ADMIN_TOKEN_PREFIX + token;
        Map<String, String> tokenInfo = new HashMap<>();
        tokenInfo.put("userId", user.getId());
        tokenInfo.put("clientType", clientType);
        tokenInfo.put("loginTime", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().putAll(tokenKey, tokenInfo);
        redisTemplate.expire(tokenKey, LoginConstant.ADMIN_TOKEN_TTL, TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(LoginConstant.ADMIN_USER_TOKEN_PREFIX + user.getId(), token);

        saveLoginLog(user, user.getUserNum(), user.getUsername(), clientType, token, (byte) 1, null, httpRequest);
        return buildLoginVO(user, token);
    }

    public AdminLoginVO me(String userId, String token) {
        ImUser user = imUserService.selectByUserId(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return buildLoginVO(user, token);
    }

    public String checkAndRefresh(String token) {
        if (!JwtUtil.validate(token)) {
            throw new RuntimeException("token非法");
        }
        String tokenKey = LoginConstant.ADMIN_TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
        if (StringUtils.isBlank(userId)) {
            throw new RuntimeException("token已过期");
        }
        redisTemplate.expire(tokenKey, LoginConstant.ADMIN_TOKEN_TTL, TimeUnit.SECONDS);
        return userId;
    }

    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        invalidateToken(token, (byte) 3, LocalDateTime.now(ZoneOffset.UTC));
    }

    public List<String> getMenuCodes(String userId) {
        return adminPermissionService.getUserMenuCodes(userId).stream().toList();
    }

    public boolean isSuperAdmin(String userId) {
        return adminPermissionService.isSuperAdmin(userId);
    }

    public Set<String> getActiveTokens(String userId) {
        Set<String> tokens = redisTemplate.opsForSet().members(LoginConstant.ADMIN_USER_TOKEN_PREFIX + userId);
        return tokens == null ? Set.of() : tokens;
    }

    public void kickByToken(String userId, String token) {
        validateTargetUser(userId);
        invalidateToken(token, (byte) 2, null);
    }

    public void kickByClientType(String userId, String clientType) {
        validateTargetUser(userId);
        if (StringUtils.isBlank(clientType)) {
            return;
        }
        Set<String> tokens = getActiveTokens(userId);
        for (String token : tokens) {
            String tokenKey = LoginConstant.ADMIN_TOKEN_PREFIX + token;
            String type = (String) redisTemplate.opsForHash().get(tokenKey, "clientType");
            if (clientType.equals(type)) {
                invalidateToken(token, (byte) 2, null);
            }
        }
    }

    public void kickAll(String userId) {
        validateTargetUser(userId);
        for (String token : getActiveTokens(userId)) {
            invalidateToken(token, (byte) 2, null);
        }
        redisTemplate.delete(LoginConstant.ADMIN_USER_TOKEN_PREFIX + userId);
    }

    private AdminLoginVO buildLoginVO(ImUser user, String token) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.setUserId(user.getId());
        vo.setUserNum(user.getUserNum());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setToken(token);
        vo.setSuperAdmin(Boolean.TRUE.equals(user.getIsAdmin()));
        vo.setMenuCodes(getMenuCodes(user.getId()));
        return vo;
    }

    private void invalidateToken(String token, Byte status, LocalDateTime logoutTime) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        String tokenKey = LoginConstant.ADMIN_TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
        if (StringUtils.isNotBlank(userId)) {
            redisTemplate.delete(tokenKey);
            redisTemplate.opsForSet().remove(LoginConstant.ADMIN_USER_TOKEN_PREFIX + userId, token);
        }
        AdminLoginLog log = adminLoginLogService.getOne(new LambdaQueryWrapper<AdminLoginLog>()
                .eq(AdminLoginLog::getToken, token)
                .eq(AdminLoginLog::getIsDeleted, false)
                .orderByDesc(AdminLoginLog::getCreatedAt)
                .last("limit 1"));
        if (log != null) {
            log.setStatus(status);
            log.setLogoutTime(logoutTime == null ? LocalDateTime.now(ZoneOffset.UTC) : logoutTime);
            adminLoginLogService.updateById(log);
        }
    }

    private void saveLoginLog(ImUser user,
                              String userNum,
                              String username,
                              String clientType,
                              String token,
                              Byte status,
                              String failReason,
                              HttpServletRequest request) {
        AdminLoginLog log = new AdminLoginLog();
        log.setId(uuid32());
        log.setUserId(user == null ? null : user.getId());
        log.setUserNum(user == null ? userNum : user.getUserNum());
        log.setUsername(user == null ? username : user.getUsername());
        log.setLoginType("ADMIN");
        log.setClientType(StringUtils.defaultIfBlank(clientType, "admin-web"));
        log.setToken(token);
        log.setLoginTime(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC));
        log.setStatus(status);
        log.setFailReason(failReason);
        log.setIp(resolveIp(request));
        log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));
        log.setIsDeleted(false);
        adminLoginLogService.save(log);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }

    private void validateTargetUser(String userId) {
        if (imUserService.selectByUserId(userId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
