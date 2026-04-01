package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.dto.AdminUserMenuUpdateRequest;
import io.github.xiaotang.novartc.server.admin.service.AdminConsoleService;
import io.github.xiaotang.novartc.server.admin.vo.AdminUserMenuVO;
import io.github.xiaotang.novartc.server.admin.vo.AdminUserVO;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminConsoleService adminConsoleService;

    @GetMapping
    @AdminPermission("user:manage")
    public List<AdminUserVO> list() {
        return adminConsoleService.listUsers(AdminContextHolder.getUserId());
    }

    @GetMapping("/{userId}")
    @AdminPermission("user:manage")
    public AdminUserVO detail(@PathVariable("userId") String userId) {
        return adminConsoleService.getUser(AdminContextHolder.getUserId(), userId);
    }

    @GetMapping("/{userId}/menus")
    @AdminPermission("permission:manage")
    public AdminUserMenuVO getMenus(@PathVariable("userId") String userId) {
        return adminConsoleService.getUserMenus(AdminContextHolder.getUserId(), userId);
    }

    @PutMapping("/{userId}/menus")
    @AdminPermission("permission:manage")
    public void updateMenus(@PathVariable("userId") String userId, @RequestBody AdminUserMenuUpdateRequest request) {
        adminConsoleService.updateUserMenus(AdminContextHolder.getUserId(), userId, request.getMenuIds());
    }
}
