package io.github.xiaotang.novartc.server.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.common.constant.LoginConstant;
import io.github.xiaotang.novartc.common.enums.FriendRequestStatus;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.admin.vo.*;
import io.github.xiaotang.novartc.server.dto.ChatHistoryPageVO;
import io.github.xiaotang.novartc.server.dto.ChatHistoryVO;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ContactVO;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.entity.mysql.*;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.repository.mongodb.IMReadStatusRepository;
import io.github.xiaotang.novartc.server.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminConsoleService {
    private final IImUserService imUserService;
    private final IImSingleChatService imSingleChatService;
    private final IImGroupChatService imGroupChatService;
    private final IImGroupMemberService imGroupMemberService;
    private final IImFriendRequestService imFriendRequestService;
    private final IImFriendService imFriendService;
    private final IAdminLoginLogService adminLoginLogService;
    private final IAdminOperateLogService adminOperateLogService;
    private final IMMessageRepository imMessageRepository;
    private final IMReadStatusRepository imReadStatusRepository;
    private final UserChannelManager userChannelManager;
    private final LoginService loginService;
    private final AdminAuthService adminAuthService;
    private final AdminPermissionService adminPermissionService;
    private final StringRedisTemplate redisTemplate;
    private final UserRelationCacheService userRelationCacheService;
    private final FriendRequestNotificationService friendRequestNotificationService;

    public List<AdminUserVO> listUsers(String currentUserId) {
        List<ImUser> users = adminPermissionService.isSuperAdmin(currentUserId)
                ? imUserService.list(new LambdaQueryWrapper<ImUser>().eq(ImUser::getIsDeleted, false).orderByAsc(ImUser::getUserNum))
                : Optional.ofNullable(imUserService.selectByUserId(currentUserId)).map(List::of).orElse(List.of());
        return users.stream().map(this::toAdminUserVO).toList();
    }

    public AdminUserVO getUser(String currentUserId, String targetUserId) {
        assertCanAccessUser(currentUserId, targetUserId);
        ImUser user = imUserService.selectByUserId(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        return toAdminUserVO(user);
    }

    public AdminUserMenuVO getUserMenus(String currentUserId, String targetUserId) {
        assertCanManageMenus(currentUserId, targetUserId);
        AdminUserMenuVO vo = new AdminUserMenuVO();
        vo.setUserId(targetUserId);
        vo.setMenuIds(adminPermissionService.getGrantedMenuIds(targetUserId));
        vo.setMenuCodes(new ArrayList<>(adminPermissionService.getUserMenuCodes(targetUserId)));
        return vo;
    }

    public void updateUserMenus(String currentUserId, String targetUserId, List<String> menuIds) {
        assertCanManageMenus(currentUserId, targetUserId);
        adminPermissionService.replaceUserMenus(targetUserId, menuIds);
    }

    public List<AdminSessionVO> listImSessions(String currentUserId, String targetUserId) {
        assertCanAccessUser(currentUserId, targetUserId);
        ImUser user = imUserService.selectByUserId(targetUserId);
        if (user == null) {
            return List.of();
        }
        return getRedisSessions(
                LoginConstant.USER_TOKEN_PREFIX + targetUserId,
                LoginConstant.TOKEN_PREFIX,
                user,
                "IM",
                userChannelManager.isOnline(targetUserId)
        );
    }

    public List<AdminSessionVO> listAdminSessions(String currentUserId) {
        List<ImUser> users = adminPermissionService.isSuperAdmin(currentUserId)
                ? imUserService.list(new LambdaQueryWrapper<ImUser>().eq(ImUser::getIsDeleted, false))
                : Optional.ofNullable(imUserService.selectByUserId(currentUserId)).map(List::of).orElse(List.of());
        List<AdminSessionVO> result = new ArrayList<>();
        for (ImUser user : users) {
            result.addAll(getRedisSessions(
                    LoginConstant.ADMIN_USER_TOKEN_PREFIX + user.getId(),
                    LoginConstant.ADMIN_TOKEN_PREFIX,
                    user,
                    "ADMIN",
                    true
            ));
        }
        result.sort(Comparator.comparing(AdminSessionVO::getLoginTime, Comparator.nullsLast(Long::compareTo)).reversed());
        return result;
    }

    public void kickImToken(String currentUserId, String targetUserId, String token) {
        assertCanAccessUser(currentUserId, targetUserId);
        if (!adminPermissionService.isSuperAdmin(currentUserId) && !StringUtils.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        loginService.kick(targetUserId, token);
    }

    public void kickImClient(String currentUserId, String targetUserId, String clientType) {
        assertCanAccessUser(currentUserId, targetUserId);
        if (!adminPermissionService.isSuperAdmin(currentUserId) && !StringUtils.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        loginService.kickByClientType(targetUserId, clientType);
    }

    public void kickImAll(String currentUserId, String targetUserId) {
        assertCanAccessUser(currentUserId, targetUserId);
        if (!adminPermissionService.isSuperAdmin(currentUserId) && !StringUtils.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        loginService.kickAll(targetUserId);
    }

    public void kickAdminToken(String currentUserId, String targetUserId, String token) {
        assertCanAccessUser(currentUserId, targetUserId);
        if (!adminPermissionService.isSuperAdmin(currentUserId) && !StringUtils.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        adminAuthService.kickByToken(targetUserId, token);
    }

    public void kickAdminClient(String currentUserId, String targetUserId, String clientType) {
        assertCanAccessUser(currentUserId, targetUserId);
        if (!adminPermissionService.isSuperAdmin(currentUserId) && !StringUtils.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        adminAuthService.kickByClientType(targetUserId, clientType);
    }

    public void kickAdminAll(String currentUserId, String targetUserId) {
        assertCanAccessUser(currentUserId, targetUserId);
        if (!adminPermissionService.isSuperAdmin(currentUserId) && !StringUtils.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        adminAuthService.kickAll(targetUserId);
    }

    public List<ChatInfoVO> listChats(String currentUserId) {
        if (adminPermissionService.isSuperAdmin(currentUserId)) {
            return listAllChats();
        }
        return listUserChats(currentUserId);
    }

    public ChatHistoryPageVO chatHistory(String currentUserId, String chatId, Long beforeChatSeq, Integer limit) {
        boolean superAdmin = adminPermissionService.isSuperAdmin(currentUserId);
        if (!superAdmin && !belongsToChat(currentUserId, chatId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        int pageSize = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
        List<IMMessage> messages = imMessageRepository.findHistory(chatId, beforeChatSeq, pageSize + 1);
        boolean hasMore = messages.size() > pageSize;
        List<IMMessage> pageMessages = hasMore ? messages.subList(0, pageSize) : messages;
        List<String> messageIds = pageMessages.stream().map(IMMessage::getMessageId).filter(StringUtils::isNotBlank).toList();
        Set<String> readIds = superAdmin ? Set.of() : imReadStatusRepository.findReadMessageIds(currentUserId, messageIds);
        ChatHistoryPageVO page = new ChatHistoryPageVO();
        page.setChatId(chatId);
        page.setHasMore(hasMore);
        page.setRecords(pageMessages.stream().map(message -> toHistoryVO(message, readIds, superAdmin)).toList());
        if (!pageMessages.isEmpty()) {
            page.setNextBeforeChatSeq(pageMessages.get(pageMessages.size() - 1).getChatSeq());
        }
        return page;
    }

    public List<ChatInfoVO> listGroups(String currentUserId) {
        return listChats(currentUserId).stream().filter(item -> StringUtils.equals(item.getChatType(), "group")).toList();
    }

    public List<ContactVO> listGroupMembers(String currentUserId, String groupId) {
        if (StringUtils.isBlank(groupId)) {
            return List.of();
        }
        if (!adminPermissionService.isSuperAdmin(currentUserId)) {
            long exists = imGroupMemberService.count(new LambdaQueryWrapper<ImGroupMember>()
                    .eq(ImGroupMember::getGroupId, groupId)
                    .eq(ImGroupMember::getUserId, currentUserId)
                    .eq(ImGroupMember::getIsDeleted, false));
            if (exists <= 0) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }
        List<String> userIds = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getGroupId, groupId)
                        .eq(ImGroupMember::getIsDeleted, false))
                .stream().map(ImGroupMember::getUserId).toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return imUserService.list(new LambdaQueryWrapper<ImUser>()
                        .in(ImUser::getId, userIds)
                        .eq(ImUser::getIsDeleted, false))
                .stream().map(this::toContactVO).toList();
    }

    public List<FriendRequestVO> listFriendRequests(String currentUserId) {
        List<ImFriendRequest> requests = adminPermissionService.isSuperAdmin(currentUserId)
                ? imFriendRequestService.list(new LambdaQueryWrapper<ImFriendRequest>().eq(ImFriendRequest::getIsDeleted, false).orderByDesc(ImFriendRequest::getUpdatedAt))
                : imFriendRequestService.list(new LambdaQueryWrapper<ImFriendRequest>()
                    .eq(ImFriendRequest::getIsDeleted, false)
                    .and(w -> w.eq(ImFriendRequest::getRequesterId, currentUserId).or().eq(ImFriendRequest::getReceiverId, currentUserId))
                    .orderByDesc(ImFriendRequest::getUpdatedAt));
        return requests.stream().map(this::toFriendRequestVO).toList();
    }

    public FriendRequestVO approveFriendRequest(String currentUserId, String requestId) {
        return handleFriendRequest(currentUserId, requestId, FriendRequestStatus.ACCEPTED.name());
    }

    public FriendRequestVO rejectFriendRequest(String currentUserId, String requestId) {
        return handleFriendRequest(currentUserId, requestId, FriendRequestStatus.REJECTED.name());
    }

    public List<AdminLoginLog> listLoginLogs(String currentUserId) {
        LambdaQueryWrapper<AdminLoginLog> query = new LambdaQueryWrapper<AdminLoginLog>().eq(AdminLoginLog::getIsDeleted, false).orderByDesc(AdminLoginLog::getLoginTime);
        if (!adminPermissionService.isSuperAdmin(currentUserId)) {
            query.eq(AdminLoginLog::getUserId, currentUserId);
        }
        return adminLoginLogService.list(query);
    }

    public List<AdminOperateLog> listOperateLogs(String currentUserId) {
        LambdaQueryWrapper<AdminOperateLog> query = new LambdaQueryWrapper<AdminOperateLog>().eq(AdminOperateLog::getIsDeleted, false).orderByDesc(AdminOperateLog::getCreatedAt);
        if (!adminPermissionService.isSuperAdmin(currentUserId)) {
            query.eq(AdminOperateLog::getOperatorUserId, currentUserId);
        }
        return adminOperateLogService.list(query);
    }

    private FriendRequestVO handleFriendRequest(String currentUserId, String requestId, String status) {
        ImFriendRequest entity = imFriendRequestService.getById(requestId);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new ApplicationException("好友申请不存在");
        }
        boolean superAdmin = adminPermissionService.isSuperAdmin(currentUserId);
        if (!superAdmin && !StringUtils.equals(entity.getReceiverId(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (!StringUtils.equals(entity.getStatus(), FriendRequestStatus.PENDING.name())) {
            throw new ApplicationException("好友申请已处理");
        }
        FriendRequestStatus targetStatus = FriendRequestStatus.valueOf(status);
        entity.setStatus(targetStatus.name());
        entity.setHandledAt(Instant.now());
        entity.setHandlerId(currentUserId);
        imFriendRequestService.updateById(entity);
        if (targetStatus == FriendRequestStatus.ACCEPTED) {
            createFriendRelation(entity.getRequesterId(), entity.getReceiverId(), entity.getId());
            createFriendRelation(entity.getReceiverId(), entity.getRequesterId(), entity.getId());
            imSingleChatService.createOrGet(entity.getRequesterId(), entity.getReceiverId());
            userRelationCacheService.rebuildUserRelationsCachesAsync(List.of(entity.getRequesterId(), entity.getReceiverId()));
        }
        ImFriendRequest latest = imFriendRequestService.getById(requestId);
        friendRequestNotificationService.notifyUpdated(latest);
        return toFriendRequestVO(latest);
    }

    private void createFriendRelation(String userId, String friendId, String requestId) {
        long exists = imFriendService.count(new LambdaQueryWrapper<ImFriend>()
                .eq(ImFriend::getIsDeleted, false)
                .eq(ImFriend::getUserId, userId)
                .eq(ImFriend::getFriendId, friendId));
        if (exists > 0) {
            return;
        }
        ImFriend friend = new ImFriend();
        friend.setId(uuid32());
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setSourceRequestId(requestId);
        friend.setIsDeleted(false);
        imFriendService.save(friend);
    }

    private List<ChatInfoVO> listAllChats() {
        List<ImSingleChat> singleChats = imSingleChatService.list(new LambdaQueryWrapper<ImSingleChat>().eq(ImSingleChat::getIsDeleted, false));
        Set<String> allUserIds = new LinkedHashSet<>();
        singleChats.forEach(chat -> {
            allUserIds.add(chat.getUser1Id());
            allUserIds.add(chat.getUser2Id());
        });
        Map<String, ImUser> userMap = allUserIds.isEmpty() ? Map.of() : imUserService.list(new LambdaQueryWrapper<ImUser>().in(ImUser::getId, allUserIds).eq(ImUser::getIsDeleted, false))
                .stream().collect(Collectors.toMap(ImUser::getId, Function.identity()));
        List<ChatInfoVO> chats = new ArrayList<>();
        for (ImSingleChat singleChat : singleChats) {
            ImUser left = userMap.get(singleChat.getUser1Id());
            ImUser right = userMap.get(singleChat.getUser2Id());
            ChatInfoVO vo = new ChatInfoVO();
            vo.setChatType("single");
            vo.setChatId(singleChat.getChatId());
            vo.setTargetUserId(singleChat.getUser2Id());
            vo.setTitle((left == null ? singleChat.getUser1Id() : left.getUsername()) + " / " + (right == null ? singleChat.getUser2Id() : right.getUsername()));
            fillLastMessage(vo, null, true);
            vo.setUnreadCount(0);
            chats.add(vo);
        }
        chats.addAll(listAllGroupChats());
        chats.sort(Comparator.comparing(ChatInfoVO::getLastMessageTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return chats;
    }

    private List<ChatInfoVO> listUserChats(String userId) {
        List<ImSingleChat> singleChats = imSingleChatService.list(new LambdaQueryWrapper<ImSingleChat>()
                .eq(ImSingleChat::getIsDeleted, false)
                .and(w -> w.eq(ImSingleChat::getUser1Id, userId).or().eq(ImSingleChat::getUser2Id, userId)));
        Set<String> targetUserIds = singleChats.stream()
                .map(chat -> StringUtils.equals(chat.getUser1Id(), userId) ? chat.getUser2Id() : chat.getUser1Id())
                .collect(Collectors.toSet());
        Map<String, ImUser> userMap = targetUserIds.isEmpty() ? Map.of() : imUserService.list(new LambdaQueryWrapper<ImUser>()
                .in(ImUser::getId, targetUserIds).eq(ImUser::getIsDeleted, false)).stream().collect(Collectors.toMap(ImUser::getId, Function.identity()));
        List<ChatInfoVO> result = new ArrayList<>();
        for (ImSingleChat singleChat : singleChats) {
            String targetUserId = StringUtils.equals(singleChat.getUser1Id(), userId) ? singleChat.getUser2Id() : singleChat.getUser1Id();
            ImUser target = userMap.get(targetUserId);
            ChatInfoVO vo = new ChatInfoVO();
            vo.setChatType("single");
            vo.setChatId(singleChat.getChatId());
            vo.setTargetUserId(targetUserId);
            vo.setTargetUserNum(target == null ? null : target.getUserNum());
            vo.setTitle(target == null ? targetUserId : target.getUsername());
            vo.setAvatarUrl(target == null ? null : target.getAvatarUrl());
            vo.setOnline(userChannelManager.isOnline(targetUserId));
            fillLastMessage(vo, userId, false);
            vo.setUnreadCount(countUnreadMessages(vo.getChatId(), userId));
            result.add(vo);
        }

        List<String> groupIds = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>()
                        .eq(ImGroupMember::getUserId, userId)
                        .eq(ImGroupMember::getIsDeleted, false))
                .stream().map(ImGroupMember::getGroupId).toList();
        if (!groupIds.isEmpty()) {
            List<ImGroupMember> groupMembers = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>().in(ImGroupMember::getGroupId, groupIds).eq(ImGroupMember::getIsDeleted, false));
            Map<String, List<ImGroupMember>> membersByGroup = groupMembers.stream().collect(Collectors.groupingBy(ImGroupMember::getGroupId));
            List<ImGroupChat> groups = imGroupChatService.list(new LambdaQueryWrapper<ImGroupChat>().in(ImGroupChat::getId, groupIds).eq(ImGroupChat::getIsDeleted, false));
            for (ImGroupChat group : groups) {
                List<ImGroupMember> members = membersByGroup.getOrDefault(group.getId(), List.of());
                ChatInfoVO vo = new ChatInfoVO();
                vo.setChatType("group");
                vo.setChatId(group.getChatId());
                vo.setGroupId(group.getId());
                vo.setGroupNum(group.getGroupNum());
                vo.setTitle(group.getGroupName());
                vo.setMemberCount(members.size());
                vo.setOnlineCount((int) members.stream().filter(member -> userChannelManager.isOnline(member.getUserId())).count());
                fillLastMessage(vo, userId, false);
                vo.setUnreadCount(countUnreadMessages(vo.getChatId(), userId));
                result.add(vo);
            }
        }
        result.sort(Comparator.comparing(ChatInfoVO::getLastMessageTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private List<ChatInfoVO> listAllGroupChats() {
        List<ImGroupChat> groups = imGroupChatService.list(new LambdaQueryWrapper<ImGroupChat>().eq(ImGroupChat::getIsDeleted, false));
        if (groups.isEmpty()) {
            return List.of();
        }
        List<String> groupIds = groups.stream().map(ImGroupChat::getId).toList();
        Map<String, List<ImGroupMember>> membersByGroup = imGroupMemberService.list(new LambdaQueryWrapper<ImGroupMember>()
                        .in(ImGroupMember::getGroupId, groupIds)
                        .eq(ImGroupMember::getIsDeleted, false))
                .stream().collect(Collectors.groupingBy(ImGroupMember::getGroupId));
        List<ChatInfoVO> result = new ArrayList<>();
        for (ImGroupChat group : groups) {
            List<ImGroupMember> members = membersByGroup.getOrDefault(group.getId(), List.of());
            ChatInfoVO vo = new ChatInfoVO();
            vo.setChatType("group");
            vo.setChatId(group.getChatId());
            vo.setGroupId(group.getId());
            vo.setGroupNum(group.getGroupNum());
            vo.setTitle(group.getGroupName());
            vo.setMemberCount(members.size());
            vo.setOnlineCount((int) members.stream().filter(member -> userChannelManager.isOnline(member.getUserId())).count());
            fillLastMessage(vo, null, true);
            vo.setUnreadCount(0);
            result.add(vo);
        }
        return result;
    }

    private void fillLastMessage(ChatInfoVO vo, String currentUserId, boolean superMode) {
        List<IMMessage> messages = imMessageRepository.findHistory(vo.getChatId(), null, 1);
        if (messages.isEmpty()) {
            return;
        }
        IMMessage message = messages.get(0);
        vo.setLastMessage(message.getMessageText());
        vo.setLastMessageType(message.getMessageType());
        vo.setLastMessageTime(message.getServerTime());
        if (!superMode && currentUserId != null) {
            vo.setUnreadCount(countUnreadMessages(vo.getChatId(), currentUserId));
        }
    }

    private int countUnreadMessages(String chatId, String userId) {
        List<String> unreadMessageIds = imMessageRepository.findOtherUserMessageIds(chatId, userId);
        if (unreadMessageIds.isEmpty()) {
            return 0;
        }
        Set<String> readIds = imReadStatusRepository.findReadMessageIds(userId, unreadMessageIds);
        return (int) unreadMessageIds.stream().filter(id -> !readIds.contains(id)).count();
    }

    private boolean belongsToChat(String userId, String chatId) {
        long singleCount = imSingleChatService.count(new LambdaQueryWrapper<ImSingleChat>()
                .eq(ImSingleChat::getChatId, chatId)
                .eq(ImSingleChat::getIsDeleted, false)
                .and(w -> w.eq(ImSingleChat::getUser1Id, userId).or().eq(ImSingleChat::getUser2Id, userId)));
        if (singleCount > 0) {
            return true;
        }
        ImGroupChat groupChat = imGroupChatService.getOne(new LambdaQueryWrapper<ImGroupChat>().eq(ImGroupChat::getChatId, chatId).eq(ImGroupChat::getIsDeleted, false).last("limit 1"));
        if (groupChat == null) {
            return false;
        }
        return imGroupMemberService.count(new LambdaQueryWrapper<ImGroupMember>()
                .eq(ImGroupMember::getGroupId, groupChat.getId())
                .eq(ImGroupMember::getUserId, userId)
                .eq(ImGroupMember::getIsDeleted, false)) > 0;
    }

    private ChatHistoryVO toHistoryVO(IMMessage message, Set<String> readIds, boolean superAdmin) {
        ChatHistoryVO vo = new ChatHistoryVO();
        vo.setMessageId(message.getMessageId());
        vo.setChatSeq(message.getChatSeq());
        vo.setChatId(message.getChatId());
        vo.setIsGroup(message.getIsGroup());
        vo.setGroupId(message.getGroupId());
        vo.setTargetUserId(message.getTargetUserId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderName(message.getSenderName());
        vo.setSenderAvatarUrl(message.getSenderAvatarUrl());
        vo.setMessageType(message.getMessageType());
        vo.setMessageText(message.getMessageText());
        vo.setServerTime(message.getServerTime());
        vo.setClientTime(message.getClientTime());
        if (StringUtils.isNotBlank(message.getContentJson())) {
            vo.setContent(JacksonUtil.toObject(message.getContentJson(), Object.class));
        }
        vo.setRead(superAdmin ? Boolean.FALSE : readIds.contains(message.getMessageId()));
        return vo;
    }

    private AdminUserVO toAdminUserVO(ImUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setUserId(user.getId());
        vo.setUserNum(user.getUserNum());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setIsAdmin(user.getIsAdmin());
        vo.setImOnline(userChannelManager.isOnline(user.getId()));
        vo.setAdminOnline(!adminAuthService.getActiveTokens(user.getId()).isEmpty());
        vo.setMenuCodes(new ArrayList<>(adminPermissionService.getUserMenuCodes(user.getId())));
        return vo;
    }

    private ContactVO toContactVO(ImUser user) {
        ContactVO vo = new ContactVO();
        vo.setUserId(user.getId());
        vo.setUserNum(user.getUserNum());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setOnline(userChannelManager.isOnline(user.getId()));
        return vo;
    }

    private FriendRequestVO toFriendRequestVO(ImFriendRequest request) {
        FriendRequestVO vo = new FriendRequestVO();
        vo.setRequestId(request.getId());
        vo.setStatus(request.getStatus());
        vo.setRequestMsg(request.getRequestMsg());
        vo.setHandledAt(request.getHandledAt());
        vo.setHandlerId(request.getHandlerId());
        vo.setCreatedAt(request.getCreatedAt());
        vo.setUpdatedAt(request.getUpdatedAt());
        fillUserInfo(vo, request.getRequesterId(), true);
        fillUserInfo(vo, request.getReceiverId(), false);
        return vo;
    }

    private void fillUserInfo(FriendRequestVO vo, String userId, boolean requester) {
        ImUser user = imUserService.selectByUserId(userId);
        if (requester) {
            vo.setRequesterId(userId);
            vo.setRequesterUserNum(user == null ? null : user.getUserNum());
            vo.setRequesterUsername(user == null ? null : user.getUsername());
            vo.setRequesterAvatarUrl(user == null ? null : user.getAvatarUrl());
            return;
        }
        vo.setReceiverId(userId);
        vo.setReceiverUserNum(user == null ? null : user.getUserNum());
        vo.setReceiverUsername(user == null ? null : user.getUsername());
        vo.setReceiverAvatarUrl(user == null ? null : user.getAvatarUrl());
    }

    private List<AdminSessionVO> getRedisSessions(String userTokensKey,
                                                 String tokenPrefix,
                                                 ImUser user,
                                                 String source,
                                                 boolean onlineStatus) {
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<AdminSessionVO> sessions = new ArrayList<>();
        for (String token : tokens) {
            String tokenKey = tokenPrefix + token;
            Object userId = redisTemplate.opsForHash().get(tokenKey, "userId");
            if (userId == null) {
                continue;
            }
            AdminSessionVO vo = new AdminSessionVO();
            vo.setUserId(user.getId());
            vo.setUserNum(user.getUserNum());
            vo.setUsername(user.getUsername());
            vo.setToken(token);
            vo.setClientType((String) redisTemplate.opsForHash().get(tokenKey, "clientType"));
            Object loginTime = redisTemplate.opsForHash().get(tokenKey, "loginTime");
            vo.setLoginTime(loginTime == null ? null : Long.parseLong(loginTime.toString()));
            vo.setOnline("IM".equals(source) ? onlineStatus : redisTemplate.hasKey(tokenKey));
            vo.setSource(source);
            sessions.add(vo);
        }
        sessions.sort(Comparator.comparing(AdminSessionVO::getLoginTime, Comparator.nullsLast(Long::compareTo)).reversed());
        return sessions;
    }

    private void assertCanAccessUser(String currentUserId, String targetUserId) {
        if (StringUtils.isBlank(targetUserId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (!adminPermissionService.isSuperAdmin(currentUserId) && !StringUtils.equals(currentUserId, targetUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void assertCanManageMenus(String currentUserId, String targetUserId) {
        if (!adminPermissionService.isSuperAdmin(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (imUserService.selectByUserId(targetUserId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
    }

    private String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
