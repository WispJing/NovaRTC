package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.constant.LoginConstant;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.common.util.JwtUtil;
import io.github.xiaotang.novartc.common.util.MD5Util;
import io.github.xiaotang.novartc.server.admin.service.RiskControlService;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.queue.TokenKickDispatchService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 登录服务，负责 IM 端登录、续期、登出和踢端控制。
 */
@Service
@RequiredArgsConstructor
public class LoginService {
    private final Logger logger = LoggerFactory.getLogger(LoginService.class);
    private final StringRedisTemplate redisTemplate;
    private final IImUserService imUserService;
    private final TokenKickDispatchService tokenKickDispatchService;
    private final UserRelationCacheService userRelationCacheService;
    private final RiskControlService riskControlService;

    /**
     * 登录（支持多端）
     */
    public LoginResponse login(String userNum, String password, String clientType, String ip, String userAgent) {
        ImUser imUser = imUserService.selectByUserNum(userNum);
        if (imUser == null) {
            riskControlService.recordPasswordError(null, userNum, null, clientType, ip, userAgent);
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }
        String userId = imUser.getId();
        if (!MD5Util.verify(password, imUser.getPassword())) {
            riskControlService.recordPasswordError(imUser, imUser.getUserNum(), imUser.getUsername(), clientType, ip, userAgent);
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        riskControlService.assertLoginAllowed(imUser, LoginConstant.LOGIN_SCOPE_IM, clientType, ip, userAgent);

        String token = JwtUtil.generateToken(userId, Map.of("clientType", clientType));

        // 使用 Hash 存储 token 的详细信息
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        Map<String, String> tokenInfo = new HashMap<>();
        tokenInfo.put(LoginConstant.TOKEN_INFO_USER_ID, userId);
        tokenInfo.put(LoginConstant.TOKEN_INFO_CLIENT_TYPE, clientType);
        tokenInfo.put(LoginConstant.TOKEN_INFO_LOGIN_TIME, String.valueOf(System.currentTimeMillis()));

        // 踢出同端登录用户
        kickByClientType(userId, clientType);

        redisTemplate.opsForHash().putAll(tokenKey, tokenInfo);
        redisTemplate.expire(tokenKey, LoginConstant.TOKEN_TTL, TimeUnit.SECONDS);

        // 将 token 加入用户的 Set 集合（多端）
        redisTemplate.opsForSet().add(LoginConstant.USER_TOKEN_PREFIX + userId, token);
        userRelationCacheService.rebuildUserRelationsCacheAsync(userId);

        return LoginResponse.of(imUser, token);
    }

    /**
     * 校验 token 是否有效。
     */
    public Boolean checkToken(String token) {
        if (!JwtUtil.validate(token)) {
            return false;
        }

        // 2. Redis校验
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, LoginConstant.TOKEN_INFO_USER_ID);

        return userId != null;
    }

    /**
     * 校验 + 续期（核心）
     */
    public String checkAndRefresh(String token) {
        // 1. JWT 校验
        if (!JwtUtil.validate(token)) {
            throw new RuntimeException("令牌无效");
        }

        // 2. 从 Redis Hash 中获取 userId
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, LoginConstant.TOKEN_INFO_USER_ID);

        if (userId == null) {
            throw new RuntimeException("令牌已过期");
        }

        // 3. 刷新整个 Hash 的 TTL
        redisTemplate.expire(tokenKey, LoginConstant.TOKEN_TTL, TimeUnit.SECONDS);

        return userId;
    }

    /**
     * 登出
     */
    public void logout(String token) {
        String currentUserId = UserContextHolder.getUserId();
        ImUser currentUser = imUserService.selectByUserId(currentUserId);
        // 登出他人需做权限控制
        if (StringUtils.isNotBlank(token)) {
            if (currentUser == null || !Boolean.TRUE.equals(currentUser.getIsAdmin())) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }
        token = UserContextHolder.getToken();
        if (StringUtils.isBlank(token)) {
            return;
        }

        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, LoginConstant.TOKEN_INFO_USER_ID);
        String clientType = (String) redisTemplate.opsForHash().get(tokenKey, LoginConstant.TOKEN_INFO_CLIENT_TYPE);
        if (userId != null) {
            // 删除 Hash
            redisTemplate.delete(tokenKey);
            // 从用户集合移除
            redisTemplate.opsForSet().remove(LoginConstant.USER_TOKEN_PREFIX + userId, token);
            logger.info("用户：{}，设备：{} 已登出", userId, clientType);
        }
        // 发送踢人消息
        TokenKickMessage kickMsg = new TokenKickMessage(userId, token);
        tokenKickDispatchService.dispatch(kickMsg);
    }

    /**
     * 踢掉指定 token 对应的在线会话。
     */
    public void kick(String userId, String token) {
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        redisTemplate.delete(tokenKey);
        redisTemplate.opsForSet().remove(LoginConstant.USER_TOKEN_PREFIX + userId, token);
        tokenKickDispatchService.dispatch(new TokenKickMessage(userId, token));
    }

    /**
     * 踢掉用户指定客户端类型的所有在线会话。
     */
    public void kickByClientType(String userId, String clientType) {
        String userTokensKey = LoginConstant.USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens != null) {
            for (String token : tokens) {
                String tokenKey = LoginConstant.TOKEN_PREFIX + token;
                String type = (String) redisTemplate.opsForHash().get(tokenKey, LoginConstant.TOKEN_INFO_CLIENT_TYPE);
                if (clientType.equals(type)) {
                    kick(userId, token);
                }
            }
        }
    }

    /**
     * 踢掉用户的全部在线设备。
     */
    public void kickAll(String userId) {
        String userTokensKey = LoginConstant.USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens != null) {
            for (String token : tokens) {
                redisTemplate.delete(LoginConstant.TOKEN_PREFIX + token);
                tokenKickDispatchService.dispatch(new TokenKickMessage(userId, token));
            }
        }
        redisTemplate.delete(userTokensKey);
    }
}
