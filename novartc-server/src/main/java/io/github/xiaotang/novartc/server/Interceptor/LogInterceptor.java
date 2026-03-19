package io.github.xiaotang.novartc.server.Interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Created on 2026/3/19 15:23
 *
 * @author tangj
 * @description
 */
@Component
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LogInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录请求开始时间
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 获取开始时间
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        // 收集关键信息
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;
        int status = response.getStatus();
        // TODO 获取真实IP
        String clientIp = request.getRemoteAddr();
        String exceptionInfo = ex != null ? ex.toString() : "none";

        // 日志模板：包含方法、路径、状态、耗时、客户端IP、异常
        logger.info("请求完成: method={}, path={}, status={}, duration={}ms, clientIp={}",
                method, fullPath, status, duration, clientIp);
    }
}
