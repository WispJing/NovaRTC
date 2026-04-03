package io.github.xiaotang.novartc.server.Interceptor;

import io.github.xiaotang.novartc.common.util.JacksonUtil;
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
    private static final String START_TIME_ATTR = "startTime";
    private static final String RESPONSE_BODY_ATTR = "responseBody";
    private static final int MAX_RESPONSE_LOG_LENGTH = 1000;
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (!logger.isInfoEnabled()) {
            return;
        }

        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;
        int status = response.getStatus();
        String clientIp = getClientIp(request);
        String exceptionInfo = ex != null ? ex.toString() : "无";

        boolean shouldLogResponse = status >= 400 || ex != null || duration >= SLOW_REQUEST_THRESHOLD_MS;
        String responseBody = shouldLogResponse
                ? serializeResponseBody(request.getAttribute(RESPONSE_BODY_ATTR))
                : "未记录";

        logger.info("请求完成: 请求方式={}, 请求路径={}, 响应状态={}, 耗时={}ms, 客户端IP={}, 响应体={}, 异常信息={}",
                method, fullPath, status, duration, clientIp, responseBody, exceptionInfo);
    }

    private String serializeResponseBody(Object responseBody) {
        if (responseBody == null) {
            return "无";
        }

        try {
            String text = responseBody instanceof String
                    ? (String) responseBody
                    : JacksonUtil.getMapper().writeValueAsString(responseBody);
            return truncate(text);
        } catch (Exception e) {
            logger.warn("响应结果序列化失败: {}", e.getMessage(), e);
            return responseBody.getClass().getSimpleName();
        }
    }

    private String truncate(String text) {
        return text.length() <= MAX_RESPONSE_LOG_LENGTH
                ? text
                : text.substring(0, MAX_RESPONSE_LOG_LENGTH) + "...（已截断）";
    }

    /**
     * 获取真实客户端 IP（支持代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
