package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.dto.AdminKickClientRequest;
import io.github.xiaotang.novartc.server.admin.dto.AdminKickTokenRequest;
import io.github.xiaotang.novartc.server.admin.service.AdminConsoleService;
import io.github.xiaotang.novartc.server.admin.vo.AdminSessionVO;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台在线管理控制器，提供 IM 会话和后台会话查询与踢下线能力。
 */
@RestController
@RequestMapping("/admin/online")
@RequiredArgsConstructor
public class AdminOnlineController {
    private final AdminConsoleService adminConsoleService;

    /**
     * 查询指定用户的 IM 在线会话。
     */
    @GetMapping("/im-users/{userId}/sessions")
    @AdminPermission("online:view")
    public List<AdminSessionVO> imSessions(@PathVariable("userId") String userId) {
        return adminConsoleService.listImSessions(AdminContextHolder.getUserId(), userId);
    }

    /**
     * 查询当前后台有效登录会话。
     */
    @GetMapping("/admin-users")
    @AdminPermission("online:view")
    public List<AdminSessionVO> adminSessions() {
        return adminConsoleService.listAdminSessions(AdminContextHolder.getUserId());
    }

    /**
     * 按 token 踢掉指定 IM 登录会话。
     */
    @PostMapping("/im-users/kick-token")
    @AdminPermission("online:kick")
    public void kickImToken(@RequestBody AdminKickTokenRequest request) {
        adminConsoleService.kickImToken(AdminContextHolder.getUserId(), request.getUserId(), request.getToken());
    }

    /**
     * 按客户端类型踢掉指定用户的 IM 登录会话。
     */
    @PostMapping("/im-users/kick-client")
    @AdminPermission("online:kick")
    public void kickImClient(@RequestBody AdminKickClientRequest request) {
        adminConsoleService.kickImClient(AdminContextHolder.getUserId(), request.getUserId(), request.getClientType());
    }

    /**
     * 踢掉指定用户的全部 IM 登录会话。
     */
    @PostMapping("/im-users/{userId}/kick-all")
    @AdminPermission("online:kick")
    public void kickImAll(@PathVariable("userId") String userId) {
        adminConsoleService.kickImAll(AdminContextHolder.getUserId(), userId);
    }

    /**
     * 按 token 踢掉指定后台登录会话。
     */
    @PostMapping("/admin-users/kick-token")
    @AdminPermission("online:kick")
    public void kickAdminToken(@RequestBody AdminKickTokenRequest request) {
        adminConsoleService.kickAdminToken(AdminContextHolder.getUserId(), request.getUserId(), request.getToken());
    }

    /**
     * 按客户端类型踢掉指定后台登录会话。
     */
    @PostMapping("/admin-users/kick-client")
    @AdminPermission("online:kick")
    public void kickAdminClient(@RequestBody AdminKickClientRequest request) {
        adminConsoleService.kickAdminClient(AdminContextHolder.getUserId(), request.getUserId(), request.getClientType());
    }

    /**
     * 踢掉指定用户的全部后台登录会话。
     */
    @PostMapping("/admin-users/{userId}/kick-all")
    @AdminPermission("online:kick")
    public void kickAdminAll(@PathVariable("userId") String userId) {
        adminConsoleService.kickAdminAll(AdminContextHolder.getUserId(), userId);
    }
}
