package io.github.xiaotang.novartc.server.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.constant.LoginConstant;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskAlertHandleRequest;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskBlacklistRequest;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskGroupMuteRequest;
import io.github.xiaotang.novartc.server.admin.dto.AdminRiskUserControlRequest;
import io.github.xiaotang.novartc.server.admin.vo.RiskBlacklistVO;
import io.github.xiaotang.novartc.server.admin.vo.RiskGroupMuteVO;
import io.github.xiaotang.novartc.server.admin.vo.RiskLoginAlertVO;
import io.github.xiaotang.novartc.server.admin.vo.RiskUserControlVO;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.entity.mysql.RiskBlacklist;
import io.github.xiaotang.novartc.server.entity.mysql.RiskGroupMute;
import io.github.xiaotang.novartc.server.entity.mysql.RiskLoginAlert;
import io.github.xiaotang.novartc.server.entity.mysql.RiskUserControl;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.service.IRiskBlacklistService;
import io.github.xiaotang.novartc.server.service.IRiskGroupMuteService;
import io.github.xiaotang.novartc.server.service.IRiskLoginAlertService;
import io.github.xiaotang.novartc.server.service.IRiskUserControlService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskControlService {
    private final IRiskUserControlService riskUserControlService;
    private final IRiskGroupMuteService riskGroupMuteService;
    private final IRiskBlacklistService riskBlacklistService;
    private final IRiskLoginAlertService riskLoginAlertService;
    private final IImUserService imUserService;
    private final IImGroupChatService imGroupChatService;
    private final StringRedisTemplate redisTemplate;
    private final RiskControlCacheService riskControlCacheService;
    private final RiskControlEnforcementService riskControlEnforcementService;

    public List<RiskUserControlVO> listUserControls() {
        List<RiskUserControl> items = riskUserControlService.list(new LambdaQueryWrapper<RiskUserControl>()
                .eq(RiskUserControl::getIsDeleted, false)
                .orderByDesc(RiskUserControl::getCreatedAt));
        Map<String, ImUser> userMap = getUserMap(items.stream().map(RiskUserControl::getUserId).filter(StringUtils::isNotBlank).collect(Collectors.toSet()));
        return items.stream().map(item -> toUserControlVO(item, userMap.get(item.getUserId()))).toList();
    }

    public RiskUserControlVO banLogin(String operatorUserId, String operatorName, AdminRiskUserControlRequest request) {
        validateUser(request.getUserId());
        String scope = StringUtils.defaultIfBlank(request.getScope(), "IM");
        closeUserControls(request.getUserId(), "LOGIN_BAN", scope);
        RiskUserControl entity = new RiskUserControl();
        entity.setId(uuid32());
        entity.setUserId(request.getUserId());
        entity.setControlType("LOGIN_BAN");
        entity.setScope(scope);
        entity.setReason(request.getReason());
        entity.setExpiresAt(toLocalDateTime(request.getExpiresAt()));
        entity.setStatus((byte) 1);
        entity.setOperatorUserId(operatorUserId);
        entity.setOperatorName(resolveOperatorName(operatorUserId, operatorName));
        entity.setIsDeleted(false);
        riskUserControlService.save(entity);
        riskControlCacheService.refreshUserControlCache(request.getUserId());
        riskControlEnforcementService.forceLogoutForLoginBan(request.getUserId(), scope);
        ImUser user = imUserService.selectByUserId(request.getUserId());
        return toUserControlVO(entity, user);
    }

    public RiskUserControlVO muteUser(String operatorUserId, String operatorName, AdminRiskUserControlRequest request) {
        validateUser(request.getUserId());
        closeUserControls(request.getUserId(), "SEND_MUTE", "ALL");
        RiskUserControl entity = new RiskUserControl();
        entity.setId(uuid32());
        entity.setUserId(request.getUserId());
        entity.setControlType("SEND_MUTE");
        entity.setScope("ALL");
        entity.setReason(request.getReason());
        entity.setExpiresAt(toLocalDateTime(request.getExpiresAt()));
        entity.setStatus((byte) 1);
        entity.setOperatorUserId(operatorUserId);
        entity.setOperatorName(resolveOperatorName(operatorUserId, operatorName));
        entity.setIsDeleted(false);
        riskUserControlService.save(entity);
        riskControlCacheService.refreshUserControlCache(request.getUserId());
        ImUser user = imUserService.selectByUserId(request.getUserId());
        return toUserControlVO(entity, user);
    }

    public void disableUserControl(String controlId) {
        RiskUserControl entity = riskUserControlService.getById(controlId);
        if (entity == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        entity.setStatus((byte) 0);
        riskUserControlService.updateById(entity);
        riskControlCacheService.refreshUserControlCache(entity.getUserId());
    }

    public List<RiskGroupMuteVO> listGroupMutes() {
        List<RiskGroupMute> items = riskGroupMuteService.list(new LambdaQueryWrapper<RiskGroupMute>()
                .eq(RiskGroupMute::getIsDeleted, false)
                .orderByDesc(RiskGroupMute::getCreatedAt));
        Map<String, ImUser> userMap = getUserMap(items.stream().map(RiskGroupMute::getUserId).filter(StringUtils::isNotBlank).collect(Collectors.toSet()));
        Map<String, ImGroupChat> groupMap = getGroupMap(items.stream().map(RiskGroupMute::getGroupId).filter(StringUtils::isNotBlank).collect(Collectors.toSet()));
        return items.stream().map(item -> toGroupMuteVO(item, groupMap.get(item.getGroupId()), userMap.get(item.getUserId()))).toList();
    }

    public RiskGroupMuteVO muteGroupAll(String operatorUserId, String operatorName, AdminRiskGroupMuteRequest request) {
        validateGroup(request.getGroupId());
        closeGroupMutes(request.getGroupId(), null, "GROUP_ALL");
        RiskGroupMute entity = new RiskGroupMute();
        entity.setId(uuid32());
        entity.setGroupId(request.getGroupId());
        entity.setMuteScope("GROUP_ALL");
        entity.setReason(request.getReason());
        entity.setExpiresAt(toLocalDateTime(request.getExpiresAt()));
        entity.setStatus((byte) 1);
        entity.setOperatorUserId(operatorUserId);
        entity.setOperatorName(resolveOperatorName(operatorUserId, operatorName));
        entity.setIsDeleted(false);
        riskGroupMuteService.save(entity);
        riskControlCacheService.refreshGroupMuteCache(request.getGroupId());
        return toGroupMuteVO(entity, imGroupChatService.getById(request.getGroupId()), null);
    }

    public RiskGroupMuteVO muteGroupUser(String operatorUserId, String operatorName, AdminRiskGroupMuteRequest request) {
        validateGroup(request.getGroupId());
        validateUser(request.getUserId());
        closeGroupMutes(request.getGroupId(), request.getUserId(), "USER");
        RiskGroupMute entity = new RiskGroupMute();
        entity.setId(uuid32());
        entity.setGroupId(request.getGroupId());
        entity.setUserId(request.getUserId());
        entity.setMuteScope("USER");
        entity.setReason(request.getReason());
        entity.setExpiresAt(toLocalDateTime(request.getExpiresAt()));
        entity.setStatus((byte) 1);
        entity.setOperatorUserId(operatorUserId);
        entity.setOperatorName(resolveOperatorName(operatorUserId, operatorName));
        entity.setIsDeleted(false);
        riskGroupMuteService.save(entity);
        riskControlCacheService.refreshGroupMuteCache(request.getGroupId());
        return toGroupMuteVO(entity, imGroupChatService.getById(request.getGroupId()), imUserService.selectByUserId(request.getUserId()));
    }

    public void disableGroupMute(String muteId) {
        RiskGroupMute entity = riskGroupMuteService.getById(muteId);
        if (entity == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        entity.setStatus((byte) 0);
        riskGroupMuteService.updateById(entity);
        riskControlCacheService.refreshGroupMuteCache(entity.getGroupId());
    }

    public List<RiskBlacklistVO> listBlacklists() {
        List<RiskBlacklist> items = riskBlacklistService.list(new LambdaQueryWrapper<RiskBlacklist>()
                .eq(RiskBlacklist::getIsDeleted, false)
                .orderByDesc(RiskBlacklist::getCreatedAt));
        Set<String> userIds = new LinkedHashSet<>();
        items.forEach(item -> {
            userIds.add(item.getOwnerUserId());
            userIds.add(item.getBlockedUserId());
        });
        Map<String, ImUser> userMap = getUserMap(userIds);
        return items.stream().map(item -> toBlacklistVO(item, userMap.get(item.getOwnerUserId()), userMap.get(item.getBlockedUserId()))).toList();
    }

    public RiskBlacklistVO addBlacklist(String operatorUserId, String operatorName, AdminRiskBlacklistRequest request) {
        validateUser(request.getOwnerUserId());
        validateUser(request.getBlockedUserId());
        RiskBlacklist existing = riskBlacklistService.getOne(new LambdaQueryWrapper<RiskBlacklist>()
                .eq(RiskBlacklist::getOwnerUserId, request.getOwnerUserId())
                .eq(RiskBlacklist::getBlockedUserId, request.getBlockedUserId())
                .eq(RiskBlacklist::getIsDeleted, false)
                .last("limit 1"));
        if (existing != null) {
            existing.setStatus((byte) 1);
            existing.setReason(request.getReason());
            existing.setSource("MANUAL");
            existing.setOperatorUserId(operatorUserId);
            existing.setOperatorName(resolveOperatorName(operatorUserId, operatorName));
            riskBlacklistService.updateById(existing);
            riskControlCacheService.refreshBlacklistCache(existing.getOwnerUserId());
            riskControlEnforcementService.forceBlockBlacklistSessions(existing.getOwnerUserId(), existing.getBlockedUserId());
            return toBlacklistVO(existing, imUserService.selectByUserId(existing.getOwnerUserId()), imUserService.selectByUserId(existing.getBlockedUserId()));
        }
        RiskBlacklist entity = new RiskBlacklist();
        entity.setId(uuid32());
        entity.setOwnerUserId(request.getOwnerUserId());
        entity.setBlockedUserId(request.getBlockedUserId());
        entity.setSource("MANUAL");
        entity.setReason(request.getReason());
        entity.setStatus((byte) 1);
        entity.setOperatorUserId(operatorUserId);
        entity.setOperatorName(resolveOperatorName(operatorUserId, operatorName));
        entity.setIsDeleted(false);
        riskBlacklistService.save(entity);
        riskControlCacheService.refreshBlacklistCache(entity.getOwnerUserId());
        riskControlEnforcementService.forceBlockBlacklistSessions(entity.getOwnerUserId(), entity.getBlockedUserId());
        return toBlacklistVO(entity, imUserService.selectByUserId(entity.getOwnerUserId()), imUserService.selectByUserId(entity.getBlockedUserId()));
    }

    public void disableBlacklist(String blacklistId) {
        RiskBlacklist entity = riskBlacklistService.getById(blacklistId);
        if (entity == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        entity.setStatus((byte) 0);
        riskBlacklistService.updateById(entity);
        riskControlCacheService.refreshBlacklistCache(entity.getOwnerUserId());
    }

    public List<RiskLoginAlertVO> listLoginAlerts() {
        List<RiskLoginAlert> items = riskLoginAlertService.list(new LambdaQueryWrapper<RiskLoginAlert>()
                .eq(RiskLoginAlert::getIsDeleted, false)
                .orderByDesc(RiskLoginAlert::getCreatedAt));
        return items.stream().map(this::toLoginAlertVO).toList();
    }

    public void handleLoginAlert(String alertId, String operatorUserId, String operatorName, AdminRiskAlertHandleRequest request) {
        RiskLoginAlert entity = riskLoginAlertService.getById(alertId);
        if (entity == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        entity.setHandledStatus(request.getHandledStatus() == null ? (byte) 1 : request.getHandledStatus());
        entity.setHandlerUserId(operatorUserId);
        entity.setHandlerName(resolveOperatorName(operatorUserId, operatorName));
        entity.setHandledAt(LocalDateTime.now(ZoneOffset.UTC));
        riskLoginAlertService.updateById(entity);
    }

    public void assertLoginAllowed(ImUser user, String scope, String clientType, String ip, String userAgent) {
        if (user == null) {
            return;
        }
        RiskUserControl activeControl = riskControlCacheService.findActiveUserControl(user.getId(), "LOGIN_BAN", scope);
        if (activeControl != null) {
            createLoginAlert(user, clientType, ip, userAgent, "LOGIN_BANNED", "HIGH", StringUtils.defaultIfBlank(activeControl.getReason(), "账号已被封禁，禁止登录"));
            throw new BusinessException(ResultCode.FORBIDDEN.code(), "账号已被封禁，禁止登录");
        }
        if (hasDifferentClientOnline(user.getId(), clientType, "ADMIN".equals(scope))) {
            createLoginAlert(user, clientType, ip, userAgent, "MULTI_CLIENT", "LOW", "同账号在其他客户端存在有效登录");
        }
    }

    public void recordPasswordError(ImUser user, String userNum, String username, String clientType, String ip, String userAgent) {
        createLoginAlert(user, userNum, username, clientType, ip, userAgent, "PASSWORD_ERROR", "MEDIUM", "密码错误或异常登录尝试");
    }

    public String validateSendMessage(String userId, ChatMessage.MsgBody msgBody) {
        RiskUserControl sendMute = riskControlCacheService.findActiveUserControl(userId, "SEND_MUTE", "ALL");
        if (sendMute != null) {
            return StringUtils.defaultIfBlank(sendMute.getReason(), "当前账号已被禁言");
        }

        if (msgBody.getIsGroup() && msgBody.hasGroupChatInfo()) {
            String groupId = msgBody.getGroupChatInfo().getGroupId();
            if (StringUtils.isBlank(groupId)) {
                return null;
            }
            RiskGroupMute groupAll = riskControlCacheService.findActiveGroupMute(groupId, null, "GROUP_ALL");
            if (groupAll != null) {
                return StringUtils.defaultIfBlank(groupAll.getReason(), "当前群已开启全员禁言");
            }
            RiskGroupMute userMute = riskControlCacheService.findActiveGroupMute(groupId, userId, "USER");
            if (userMute != null) {
                return StringUtils.defaultIfBlank(userMute.getReason(), "你已被该群禁言");
            }
            return null;
        }

        if (msgBody.hasSingleChatInfo()) {
            String targetUserId = msgBody.getSingleChatInfo().getUser2Id();
            if (StringUtils.isNotBlank(targetUserId) && riskControlCacheService.isBlacklisted(userId, targetUserId)) {
                return "当前会话受黑名单限制，无法发送消息";
            }
        }
        return null;
    }

    private void createLoginAlert(ImUser user, String clientType, String ip, String userAgent, String alertType, String alertLevel, String detail) {
        createLoginAlert(user, user == null ? null : user.getUserNum(), user == null ? null : user.getUsername(), clientType, ip, userAgent, alertType, alertLevel, detail);
    }

    private void createLoginAlert(ImUser user, String userNum, String username, String clientType, String ip, String userAgent, String alertType, String alertLevel, String detail) {
        RiskLoginAlert entity = new RiskLoginAlert();
        entity.setId(uuid32());
        entity.setUserId(user == null ? null : user.getId());
        entity.setUserNum(user == null ? userNum : user.getUserNum());
        entity.setUsername(user == null ? username : user.getUsername());
        entity.setClientType(clientType);
        entity.setIp(ip);
        entity.setUserAgent(userAgent);
        entity.setAlertType(alertType);
        entity.setAlertLevel(alertLevel);
        entity.setDetail(detail);
        entity.setHandledStatus((byte) 0);
        entity.setIsDeleted(false);
        riskLoginAlertService.save(entity);
    }

    private boolean hasDifferentClientOnline(String userId, String clientType, boolean adminScope) {
        String setKey = (adminScope ? LoginConstant.ADMIN_USER_TOKEN_PREFIX : LoginConstant.USER_TOKEN_PREFIX) + userId;
        String tokenPrefix = adminScope ? LoginConstant.ADMIN_TOKEN_PREFIX : LoginConstant.TOKEN_PREFIX;
        Set<String> tokens = redisTemplate.opsForSet().members(setKey);
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        for (String token : tokens) {
            String currentType = (String) redisTemplate.opsForHash().get(tokenPrefix + token, "clientType");
            if (StringUtils.isNotBlank(currentType) && !StringUtils.equals(currentType, clientType)) {
                return true;
            }
        }
        return false;
    }

    private void closeUserControls(String userId, String controlType, String scope) {
        List<RiskUserControl> items = riskUserControlService.list(new LambdaQueryWrapper<RiskUserControl>()
                .eq(RiskUserControl::getUserId, userId)
                .eq(RiskUserControl::getControlType, controlType)
                .eq(RiskUserControl::getScope, scope)
                .eq(RiskUserControl::getStatus, (byte) 1)
                .eq(RiskUserControl::getIsDeleted, false));
        items.forEach(item -> item.setStatus((byte) 0));
        if (!items.isEmpty()) {
            riskUserControlService.updateBatchById(items);
        }
    }

    private void closeGroupMutes(String groupId, String userId, String muteScope) {
        List<RiskGroupMute> items = riskGroupMuteService.list(new LambdaQueryWrapper<RiskGroupMute>()
                .eq(RiskGroupMute::getGroupId, groupId)
                .eq(RiskGroupMute::getMuteScope, muteScope)
                .eq(userId != null, RiskGroupMute::getUserId, userId)
                .eq(RiskGroupMute::getStatus, (byte) 1)
                .eq(RiskGroupMute::getIsDeleted, false));
        items.forEach(item -> item.setStatus((byte) 0));
        if (!items.isEmpty()) {
            riskGroupMuteService.updateBatchById(items);
        }
    }

    private boolean isActive(RiskUserControl item) {
        return item != null && Byte.valueOf((byte) 1).equals(item.getStatus()) && (item.getExpiresAt() == null || item.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
    }

    private boolean isActive(RiskGroupMute item) {
        return item != null && Byte.valueOf((byte) 1).equals(item.getStatus()) && (item.getExpiresAt() == null || item.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
    }

    private void validateUser(String userId) {
        if (imUserService.selectByUserId(userId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
    }

    private void validateGroup(String groupId) {
        if (imGroupChatService.getById(groupId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
    }

    private Map<String, ImUser> getUserMap(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return imUserService.list(new LambdaQueryWrapper<ImUser>().in(ImUser::getId, userIds).eq(ImUser::getIsDeleted, false))
                .stream().collect(Collectors.toMap(ImUser::getId, Function.identity()));
    }

    private Map<String, ImGroupChat> getGroupMap(Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }
        return imGroupChatService.list(new LambdaQueryWrapper<ImGroupChat>().in(ImGroupChat::getId, groupIds).eq(ImGroupChat::getIsDeleted, false))
                .stream().collect(Collectors.toMap(ImGroupChat::getId, Function.identity()));
    }

    private RiskUserControlVO toUserControlVO(RiskUserControl item, ImUser user) {
        RiskUserControlVO vo = new RiskUserControlVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setUserNum(user == null ? null : user.getUserNum());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setControlType(item.getControlType());
        vo.setScope(item.getScope());
        vo.setReason(item.getReason());
        vo.setExpiresAt(item.getExpiresAt());
        vo.setStatus(item.getStatus());
        vo.setActive(isActive(item));
        vo.setCreatedAt(item.getCreatedAt());
        return vo;
    }

    private RiskGroupMuteVO toGroupMuteVO(RiskGroupMute item, ImGroupChat group, ImUser user) {
        RiskGroupMuteVO vo = new RiskGroupMuteVO();
        vo.setId(item.getId());
        vo.setGroupId(item.getGroupId());
        vo.setGroupNum(group == null ? null : group.getGroupNum());
        vo.setGroupName(group == null ? null : group.getGroupName());
        vo.setUserId(item.getUserId());
        vo.setUserNum(user == null ? null : user.getUserNum());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setMuteScope(item.getMuteScope());
        vo.setReason(item.getReason());
        vo.setExpiresAt(item.getExpiresAt());
        vo.setStatus(item.getStatus());
        vo.setActive(isActive(item));
        vo.setCreatedAt(item.getCreatedAt());
        return vo;
    }

    private RiskBlacklistVO toBlacklistVO(RiskBlacklist item, ImUser owner, ImUser blocked) {
        RiskBlacklistVO vo = new RiskBlacklistVO();
        vo.setId(item.getId());
        vo.setOwnerUserId(item.getOwnerUserId());
        vo.setOwnerUserNum(owner == null ? null : owner.getUserNum());
        vo.setOwnerUsername(owner == null ? null : owner.getUsername());
        vo.setBlockedUserId(item.getBlockedUserId());
        vo.setBlockedUserNum(blocked == null ? null : blocked.getUserNum());
        vo.setBlockedUsername(blocked == null ? null : blocked.getUsername());
        vo.setSource(item.getSource());
        vo.setReason(item.getReason());
        vo.setStatus(item.getStatus());
        vo.setCreatedAt(item.getCreatedAt());
        return vo;
    }

    private RiskLoginAlertVO toLoginAlertVO(RiskLoginAlert item) {
        RiskLoginAlertVO vo = new RiskLoginAlertVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setUserNum(item.getUserNum());
        vo.setUsername(item.getUsername());
        vo.setClientType(item.getClientType());
        vo.setIp(item.getIp());
        vo.setUserAgent(item.getUserAgent());
        vo.setAlertType(item.getAlertType());
        vo.setAlertLevel(item.getAlertLevel());
        vo.setDetail(item.getDetail());
        vo.setHandledStatus(item.getHandledStatus());
        vo.setHandlerUserId(item.getHandlerUserId());
        vo.setHandlerName(item.getHandlerName());
        vo.setHandledAt(item.getHandledAt());
        vo.setCreatedAt(item.getCreatedAt());
        return vo;
    }

    private String resolveOperatorName(String operatorUserId, String operatorName) {
        if (StringUtils.isNotBlank(operatorName)) {
            return operatorName;
        }
        if (StringUtils.isBlank(operatorUserId)) {
            return null;
        }
        ImUser operator = imUserService.selectByUserId(operatorUserId);
        return operator == null ? null : operator.getUsername();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}


