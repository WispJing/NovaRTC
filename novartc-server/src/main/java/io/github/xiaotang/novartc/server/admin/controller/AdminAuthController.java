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

/**
 * 后台认证控制器，提供后台登录态、当前账号和菜单信息接口。
 */
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;

    /**
     * 后台账号登录。
     */
    @PostMapping("/login")
    public AdminLoginVO login(@RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        return adminAuthService.login(request, httpRequest);
    }

    /**
     * 后台账号退出登录。
     */
    @PostMapping("/logout")
    public void logout() {
        adminAuthService.logout(AdminContextHolder.getToken());
    }

    /**
     * 获取当前后台登录账号信息。
     */
    @GetMapping("/me")
    public AdminLoginVO me() {
        return adminAuthService.me(AdminContextHolder.getUserId(), AdminContextHolder.getToken());
    }

    /**
     * 获取当前后台账号可访问的菜单树。
     */
    @GetMapping("/menus")
    public List<AdminMenuVO> menus() {
        return adminPermissionService.getUserMenuTree(AdminContextHolder.getUserId());
    }
}
