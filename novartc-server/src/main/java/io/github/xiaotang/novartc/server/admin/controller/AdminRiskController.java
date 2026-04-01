package io.github.xiaotang.novartc.server.admin.controller;

import io.github.xiaotang.novartc.server.admin.annotation.AdminPermission;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskAlertHandleRequest;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskBlacklistRequest;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskGroupMuteRequest;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskUserControlRequest;
import io.github.xiaotang.novartc.server.admin.service.RiskControlService;
import io.github.xiaotang.novartc.server.admin.vo.RiskBlacklistVO;
import io.github.xiaotang.novartc.server.admin.vo.RiskGroupMuteVO;
import io.github.xiaotang.novartc.server.admin.vo.RiskLoginAlertVO;
import io.github.xiaotang.novartc.server.admin.vo.RiskUserControlVO;
import io.github.xiaotang.novartc.server.config.AdminContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后台风控控制器，提供账号风控、群风控、黑名单和登录告警管理接口。
 */
@RestController
@RequestMapping("/admin/risk")
@RequiredArgsConstructor
public class AdminRiskController {
    private final RiskControlService riskControlService;

    /**
     * 查询账号风控策略列表。
     */
    @GetMapping("/user-controls")
    @AdminPermission("risk:user:view")
    public List<RiskUserControlVO> userControls() {
        return riskControlService.listUserControls();
    }

    /**
     * 对指定账号新增登录封禁策略。
     */
    @PostMapping("/user-controls/login-ban")
    @AdminPermission("risk:user:edit")
    public RiskUserControlVO banLogin(@RequestBody AdminRiskUserControlRequest request) {
        return riskControlService.banLogin(AdminContextHolder.getUserId(), null, request);
    }

    /**
     * 对指定账号新增发送禁言策略。
     */
    @PostMapping("/user-controls/send-mute")
    @AdminPermission("risk:user:edit")
    public RiskUserControlVO muteUser(@RequestBody AdminRiskUserControlRequest request) {
        return riskControlService.muteUser(AdminContextHolder.getUserId(), null, request);
    }

    /**
     * 关闭指定账号风控策略。
     */
    @DeleteMapping("/user-controls/{controlId}")
    @AdminPermission("risk:user:edit")
    public void disableUserControl(@PathVariable("controlId") String controlId) {
        riskControlService.disableUserControl(controlId);
    }

    /**
     * 查询群聊风控策略列表。
     */
    @GetMapping("/group-mutes")
    @AdminPermission("risk:group:view")
    public List<RiskGroupMuteVO> groupMutes() {
        return riskControlService.listGroupMutes();
    }

    /**
     * 对指定群聊新增全员禁言策略。
     */
    @PostMapping("/group-mutes/all")
    @AdminPermission("risk:group:edit")
    public RiskGroupMuteVO muteGroupAll(@RequestBody AdminRiskGroupMuteRequest request) {
        return riskControlService.muteGroupAll(AdminContextHolder.getUserId(), null, request);
    }

    /**
     * 对指定群成员新增禁言策略。
     */
    @PostMapping("/group-mutes/user")
    @AdminPermission("risk:group:edit")
    public RiskGroupMuteVO muteGroupUser(@RequestBody AdminRiskGroupMuteRequest request) {
        return riskControlService.muteGroupUser(AdminContextHolder.getUserId(), null, request);
    }

    /**
     * 关闭指定群聊风控策略。
     */
    @DeleteMapping("/group-mutes/{muteId}")
    @AdminPermission("risk:group:edit")
    public void disableGroupMute(@PathVariable("muteId") String muteId) {
        riskControlService.disableGroupMute(muteId);
    }

    /**
     * 查询人工黑名单列表。
     */
    @GetMapping("/blacklists")
    @AdminPermission("risk:blacklist:view")
    public List<RiskBlacklistVO> blacklists() {
        return riskControlService.listBlacklists();
    }

    /**
     * 新增人工黑名单规则。
     */
    @PostMapping("/blacklists")
    @AdminPermission("risk:blacklist:edit")
    public RiskBlacklistVO addBlacklist(@RequestBody AdminRiskBlacklistRequest request) {
        return riskControlService.addBlacklist(AdminContextHolder.getUserId(), null, request);
    }

    /**
     * 关闭指定黑名单规则。
     */
    @DeleteMapping("/blacklists/{blacklistId}")
    @AdminPermission("risk:blacklist:edit")
    public void disableBlacklist(@PathVariable("blacklistId") String blacklistId) {
        riskControlService.disableBlacklist(blacklistId);
    }

    /**
     * 查询登录风险告警列表。
     */
    @GetMapping("/login-alerts")
    @AdminPermission("risk:alert:view")
    public List<RiskLoginAlertVO> loginAlerts() {
        return riskControlService.listLoginAlerts();
    }

    /**
     * 处理指定登录风险告警。
     */
    @PostMapping("/login-alerts/{alertId}/handle")
    @AdminPermission("risk:alert:handle")
    public void handleLoginAlert(@PathVariable("alertId") String alertId, @RequestBody AdminRiskAlertHandleRequest request) {
        riskControlService.handleLoginAlert(alertId, AdminContextHolder.getUserId(), null, request);
    }
}
