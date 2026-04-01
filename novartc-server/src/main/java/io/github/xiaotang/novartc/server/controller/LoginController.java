package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.common.util.UserAgentUtil;
import io.github.xiaotang.novartc.server.dto.IMUserDTO;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * IM 登录控制器，提供客户端登录和登出接口。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class LoginController {
    private final LoginService loginService;

    /**
     * IM 账号登录。
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody IMUserDTO imUserDTO,
                               HttpServletRequest request) {
        String clientType = imUserDTO.getClientType();
        if (StringUtils.isBlank(clientType)) {
            clientType = UserAgentUtil.parseClientTypeFromUserAgent(request.getHeader("User-Agent"));
        }
        return loginService.login(
                imUserDTO.getUserNum(),
                imUserDTO.getPassword(),
                clientType,
                resolveIp(request),
                request == null ? null : request.getHeader("User-Agent")
        );
    }

    /**
     * IM 账号登出。
     */
    @PostMapping("/logout")
    public void logout(@RequestBody LoginResponse loginResponse) {
        loginService.logout(loginResponse.getToken());
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
}
