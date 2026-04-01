package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.service.AdminConsoleService;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import io.github.xiaotang.novartc.server.entity.mysql.AdminLoginLog;
import io.github.xiaotang.novartc.server.entity.mysql.AdminOperateLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {
    private final AdminConsoleService adminConsoleService;

    @GetMapping("/login")
    @AdminPermission("log:view")
    public List<AdminLoginLog> loginLogs() {
        return adminConsoleService.listLoginLogs(AdminContextHolder.getUserId());
    }

    @GetMapping("/operate")
    @AdminPermission("log:view")
    public List<AdminOperateLog> operateLogs() {
        return adminConsoleService.listOperateLogs(AdminContextHolder.getUserId());
    }
}
