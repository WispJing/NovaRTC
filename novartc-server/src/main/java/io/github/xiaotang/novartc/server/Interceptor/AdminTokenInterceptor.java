package io.github.xiaotang.novartc.server.Interceptor;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.service.AdminAuthService;
import io.github.xiaotang.novartc.server.admin.service.AdminPermissionService;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminTokenInterceptor implements HandlerInterceptor {
    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String userId = adminAuthService.checkAndRefresh(token);
        AdminContextHolder.setUserId(userId);
        AdminContextHolder.setToken(token);
        AdminContextHolder.setSuperAdmin(adminAuthService.isSuperAdmin(userId));
        AdminContextHolder.setMenuCodes(Set.copyOf(adminAuthService.getMenuCodes(userId)));

        if (handler instanceof HandlerMethod handlerMethod) {
            AdminPermission permission = handlerMethod.getMethodAnnotation(AdminPermission.class);
            if (permission == null) {
                permission = handlerMethod.getBeanType().getAnnotation(AdminPermission.class);
            }
            if (permission != null && !adminPermissionService.hasPermission(userId, permission.value())) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AdminContextHolder.clear();
    }
}
