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

/**
 * 后台账号管理控制器，提供后台账号查询和页面授权接口。
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminConsoleService adminConsoleService;

    /**
     * 查询后台可管理的账号列表。
     */
    @GetMapping
    @AdminPermission("user:manage")
    public List<AdminUserVO> list() {
        return adminConsoleService.listUsers(AdminContextHolder.getUserId());
    }

    /**
     * 查询指定后台账号详情。
     */
    @GetMapping("/{userId}")
    @AdminPermission("user:manage")
    public AdminUserVO detail(@PathVariable("userId") String userId) {
        return adminConsoleService.getUser(AdminContextHolder.getUserId(), userId);
    }

    /**
     * 查询指定后台账号的菜单授权。
     */
    @GetMapping("/{userId}/menus")
    @AdminPermission("permission:manage")
    public AdminUserMenuVO getMenus(@PathVariable("userId") String userId) {
        return adminConsoleService.getUserMenus(AdminContextHolder.getUserId(), userId);
    }

    /**
     * 更新指定后台账号的菜单授权。
     */
    @PutMapping("/{userId}/menus")
    @AdminPermission("permission:manage")
    public void updateMenus(@PathVariable("userId") String userId, @RequestBody AdminUserMenuUpdateRequest request) {
        adminConsoleService.updateUserMenus(AdminContextHolder.getUserId(), userId, request.getMenuIds());
    }
}
