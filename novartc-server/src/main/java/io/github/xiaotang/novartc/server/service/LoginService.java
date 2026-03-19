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
    public LoginResponse login(String userNum, String password) {
        ImUser imUser = imUserService.selectByUserNum(userNum);
        if (imUser == null) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }
        String userId = imUser.getId();
        if (!MD5Util.verify(password, imUser.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAIL);
        }

        // 1. 生成JWT
        String token = JwtUtil.generateToken(userId, null);

        // 2. 存 token -> userId
        redisTemplate.opsForValue().set(
                LoginConstant.TOKEN_PREFIX + token,
                userId,
                LoginConstant.TOKEN_TTL,
                TimeUnit.SECONDS
        );

        // 3. 加入用户token集合（多端）
        redisTemplate.opsForSet().add(
                LoginConstant.USER_TOKEN_PREFIX + userId,
                token
        );

        return LoginResponse.of(token);
    }

    public Boolean checkToken(String token) {
        if (!JwtUtil.validate(token)) {
            return false;
        }

        // 2. Redis校验
        String key = LoginConstant.TOKEN_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(key);

        return userId != null;
    }

    /**
     * 校验 + 续期（核心）
     */
    public String checkAndRefresh(String token) {

        // 1. JWT校验
        if (!JwtUtil.validate(token)) {
            throw new RuntimeException("token非法");
        }

        // 2. Redis校验
        String key = LoginConstant.TOKEN_PREFIX + token;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new RuntimeException("token已过期");
        }

        // 3. 刷新TTL（滑动过期核心）
        redisTemplate.expire(key, LoginConstant.TOKEN_TTL, TimeUnit.SECONDS);

        return userId;
    }

    /**
     * 登出（当前设备）
     */
    public void logout(String token) {
        String key = LoginConstant.TOKEN_PREFIX + token;

        String userId = redisTemplate.opsForValue().get(key);
        if (userId != null) {
            // 删除token
            redisTemplate.delete(key);

            // 从用户集合移除
            redisTemplate.opsForSet().remove(
                    LoginConstant.USER_TOKEN_PREFIX + userId,
                    token
            );
        }
        TokenKickMessage kickMsg = new TokenKickMessage(userId, token);
        rocketMQTemplate.convertAndSend("im-token-kick", kickMsg);
    }

    // 踢掉某个设备
    public void kick(String userId, String token) {
        // 1. 删除 Redis
        redisTemplate.delete(LoginConstant.TOKEN_PREFIX + token);
        redisTemplate.opsForSet().remove(LoginConstant.USER_TOKEN_PREFIX + userId, token);

        // 2. 发送 MQ 踢人事件
        TokenKickMessage kickMsg = new TokenKickMessage(userId, token);
        rocketMQTemplate.convertAndSend("im-token-kick", kickMsg);
    }

    // 踢掉所有设备
    public void kickAll(String userId) {
        String key = LoginConstant.USER_TOKEN_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(key);
        if (tokens != null) {
            for (String token : tokens) {
                redisTemplate.delete(LoginConstant.TOKEN_PREFIX + token);
                // 发送 MQ 消息（可批量发送或循环）
                rocketMQTemplate.convertAndSend("im-token-kick", new TokenKickMessage(userId, token));
            }
        }
        redisTemplate.delete(key);
    }
}
