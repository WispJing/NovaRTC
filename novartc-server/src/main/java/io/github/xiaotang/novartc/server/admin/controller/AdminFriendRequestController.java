package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.service.AdminConsoleService;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/friend-requests")
@RequiredArgsConstructor
public class AdminFriendRequestController {
    private final AdminConsoleService adminConsoleService;

    @GetMapping
    @AdminPermission("friend-request:audit")
    public List<FriendRequestVO> list() {
        return adminConsoleService.listFriendRequests(AdminContextHolder.getUserId());
    }

    @PostMapping("/{requestId}/approve")
    @AdminPermission("friend-request:audit")
    public FriendRequestVO approve(@PathVariable("requestId") String requestId) {
        return adminConsoleService.approveFriendRequest(AdminContextHolder.getUserId(), requestId);
    }

    @PostMapping("/{requestId}/reject")
    @AdminPermission("friend-request:audit")
    public FriendRequestVO reject(@PathVariable("requestId") String requestId) {
        return adminConsoleService.rejectFriendRequest(AdminContextHolder.getUserId(), requestId);
    }
}
