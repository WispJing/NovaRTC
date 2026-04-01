package io.github.xiaotang.novartc.server.Interceptor;

import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import io.github.xiaotang.novartc.server.entity.mysql.AdminOperateLog;
import io.github.xiaotang.novartc.server.service.IAdminOperateLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminOperateLogInterceptor implements HandlerInterceptor {
    private final IAdminOperateLogService adminOperateLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String uri = request.getRequestURI();
        if (!StringUtils.startsWith(uri, "/admin/") || StringUtils.startsWith(uri, "/admin/auth/login")) {
            return;
        }
        AdminOperateLog log = new AdminOperateLog();
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setOperatorUserId(AdminContextHolder.getUserId());
        log.setModule(resolveModule(uri));
        log.setAction(request.getMethod());
        log.setRequestMethod(request.getMethod());
        log.setRequestUri(uri);
        log.setRequestParams(request.getQueryString());
        log.setSuccess((byte) (ex == null && response.getStatus() < 400 ? 1 : 0));
        log.setResultCode(String.valueOf(response.getStatus()));
        log.setResultMsg(ex == null ? "OK" : ex.getMessage());
        log.setIp(resolveIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setIsDeleted(false);
        adminOperateLogService.save(log);
    }

    private String resolveModule(String uri) {
        String path = uri.replaceFirst("^/admin/?", "");
        int index = path.indexOf('/');
        return index < 0 ? path : path.substring(0, index);
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }
}
