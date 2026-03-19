package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.constant.LoginConstant;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.common.util.JwtUtil;
import io.github.xiaotang.novartc.common.util.MD5Util;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2026/3/18 15:37
 *
 * @author tangj
 * @description
 */
@Service
@RequiredArgsConstructor
public class LoginService {
    private final RocketMQTemplate rocketMQTemplate;
    private final StringRedisTemplate redisTemplate;
    private final IImUserService imUserService;

    /**
     * 登录（支持多端）
     */
    public LoginResponse login(String userNum, String password, String clientType) {
        ImUser imUser = imUserService.selectByUserNum(userNum);
        if (imUser == null) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }
        String userId = imUser.getId();
        if (!MD5Util.verify(password, imUser.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        String token = JwtUtil.generateToken(userId, Map.of("clientType", clientType));

        // 使用 Hash 存储 token 的详细信息
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        Map<String, String> tokenInfo = new HashMap<>();
        tokenInfo.put("userId", userId);
        tokenInfo.put("clientType", clientType);
        tokenInfo.put("loginTime", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(tokenKey, tokenInfo);
        redisTemplate.expire(tokenKey, LoginConstant.TOKEN_TTL, TimeUnit.SECONDS);

        // 将 token 加入用户的 Set 集合（多端）
        redisTemplate.opsForSet().add(LoginConstant.USER_TOKEN_PREFIX + userId, token);

        return LoginResponse.of(token);
    }

    public Boolean checkToken(String token) {
        if (!JwtUtil.validate(token)) {
            return false;
        }

        // 2. Redis校验
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");

        return userId != null;
    }

    /**
     * 校验 + 续期（核心）
     */
    public String checkAndRefresh(String token) {
        // 1. JWT 校验
        if (!JwtUtil.validate(token)) {
            throw new RuntimeException("token非法");
        }

        // 2. 从 Redis Hash 中获取 userId
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");

        if (userId == null) {
            throw new RuntimeException("token已过期");
        }

        // 3. 刷新整个 Hash 的 TTL
        redisTemplate.expire(tokenKey, LoginConstant.TOKEN_TTL, TimeUnit.SECONDS);

        return userId;
    }

    /**
     * 登出（当前设备）
     */
    public void logout(String token) {
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        String userId = (String) redisTemplate.opsForHash().get(tokenKey, "userId");
        if (userId != null) {
            // 删除 Hash
            redisTemplate.delete(tokenKey);
            // 从用户集合移除
            redisTemplate.opsForSet().remove(LoginConstant.USER_TOKEN_PREFIX + userId, token);
        }
        // 发送踢人消息（可选）
        TokenKickMessage kickMsg = new TokenKickMessage(userId, token);
        rocketMQTemplate.convertAndSend("im-token-kick", kickMsg);
    }

    // 踢掉指定 token
    public void kick(String userId, String token) {
        String tokenKey = LoginConstant.TOKEN_PREFIX + token;
        redisTemplate.delete(tokenKey);
        redisTemplate.opsForSet().remove(LoginConstant.USER_TOKEN_PREFIX + userId, token);
        rocketMQTemplate.convertAndSend("im-token-kick", new TokenKickMessage(userId, token));
    }

    // 踢掉用户的所有指定类型的设备
    public void kickByClientType(String userId, String clientType) {
        String userTokensKey = LoginConstant.USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens != null) {
            for (String token : tokens) {
                String tokenKey = LoginConstant.TOKEN_PREFIX + token;
                String type = (String) redisTemplate.opsForHash().get(tokenKey, "clientType");
                if (clientType.equals(type)) {
                    kick(userId, token);
                }
            }
        }
    }

    // 踢掉所有设备
    public void kickAll(String userId) {
        String userTokensKey = LoginConstant.USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens != null) {
            for (String token : tokens) {
                redisTemplate.delete(LoginConstant.TOKEN_PREFIX + token);
                rocketMQTemplate.convertAndSend("im-token-kick", new TokenKickMessage(userId, token));
            }
        }
        redisTemplate.delete(userTokensKey);
    }
}
