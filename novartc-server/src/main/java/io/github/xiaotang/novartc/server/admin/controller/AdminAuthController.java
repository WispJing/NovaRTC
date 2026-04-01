package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.dto.AdminLoginRequest;
import io.github.xiaotang.novartc.server.admin.service.AdminAuthService;
import io.github.xiaotang.novartc.server.admin.service.AdminPermissionService;
import io.github.xiaotang.novartc.server.admin.vo.AdminLoginVO;
import io.github.xiaotang.novartc.server.admin.vo.AdminMenuVO;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;

    @PostMapping("/login")
    public AdminLoginVO login(@RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        return adminAuthService.login(request, httpRequest);
    }

    @PostMapping("/logout")
    public void logout() {
        adminAuthService.logout(AdminContextHolder.getToken());
    }

    @GetMapping("/me")
    public AdminLoginVO me() {
        return adminAuthService.me(AdminContextHolder.getUserId(), AdminContextHolder.getToken());
    }

    @GetMapping("/menus")
    public List<AdminMenuVO> menus() {
        return adminPermissionService.getUserMenuTree(AdminContextHolder.getUserId());
    }
}
