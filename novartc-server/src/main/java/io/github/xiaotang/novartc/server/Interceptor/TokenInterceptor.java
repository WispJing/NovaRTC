package io.github.xiaotang.novartc.server.Interceptor;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Created on 2026/3/19 14:56
 *
 * @author tangj
 * @description
 */
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {
    private final Logger logger = LoggerFactory.getLogger(TokenInterceptor.class);
    private final LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String userId;
        try {
            userId = loginService.checkAndRefresh(token);
        } catch (RuntimeException e) {
            logger.warn("访问令牌校验失败: {}", e.getMessage(), e);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        UserContextHolder.setUserId(userId);
        UserContextHolder.setToken(token);
//        logger.info("用户上下文已设置: userId={}, token={}", userId, token);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
//        logger.info("用户上下文已清除");
    }
}
