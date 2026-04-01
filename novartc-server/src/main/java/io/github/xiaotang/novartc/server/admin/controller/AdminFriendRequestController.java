package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.service.AdminConsoleService;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台好友申请审核控制器。
 */
@RestController
@RequestMapping("/admin/friend-requests")
@RequiredArgsConstructor
public class AdminFriendRequestController {
    private final AdminConsoleService adminConsoleService;

    /**
     * 查询好友申请审核列表。
     */
    @GetMapping
    @AdminPermission("friend-request:audit")
    public List<FriendRequestVO> list() {
        return adminConsoleService.listFriendRequests(AdminContextHolder.getUserId());
    }

    /**
     * 审批通过指定好友申请。
     */
    @PostMapping("/{requestId}/approve")
    @AdminPermission("friend-request:audit")
    public FriendRequestVO approve(@PathVariable("requestId") String requestId) {
        return adminConsoleService.approveFriendRequest(AdminContextHolder.getUserId(), requestId);
    }

    /**
     * 拒绝指定好友申请。
     */
    @PostMapping("/{requestId}/reject")
    @AdminPermission("friend-request:audit")
    public FriendRequestVO reject(@PathVariable("requestId") String requestId) {
        return adminConsoleService.rejectFriendRequest(AdminContextHolder.getUserId(), requestId);
    }
}
