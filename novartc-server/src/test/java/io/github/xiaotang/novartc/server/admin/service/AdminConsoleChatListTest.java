package io.github.xiaotang.novartc.server.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.repository.mongodb.IMReadStatusRepository;
import io.github.xiaotang.novartc.server.service.FriendRequestNotificationService;
import io.github.xiaotang.novartc.server.service.IAdminLoginLogService;
import io.github.xiaotang.novartc.server.service.IAdminOperateLogService;
import io.github.xiaotang.novartc.server.service.IImFriendRequestService;
import io.github.xiaotang.novartc.server.service.IImFriendService;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImGroupMemberService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.service.LoginService;
import io.github.xiaotang.novartc.server.service.UserRelationCacheService;
import io.github.xiaotang.novartc.server.util.MinioUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConsoleChatListTest {

    @Mock
    private IImUserService imUserService;
    @Mock
    private IImSingleChatService imSingleChatService;
    @Mock
    private IImGroupChatService imGroupChatService;
    @Mock
    private IImGroupMemberService imGroupMemberService;
    @Mock
    private IImFriendRequestService imFriendRequestService;
    @Mock
    private IImFriendService imFriendService;
    @Mock
    private IAdminLoginLogService adminLoginLogService;
    @Mock
    private IAdminOperateLogService adminOperateLogService;
    @Mock
    private IMMessageRepository imMessageRepository;
    @Mock
    private IMReadStatusRepository imReadStatusRepository;
    @Mock
    private UserChannelManager userChannelManager;
    @Mock
    private LoginService loginService;
    @Mock
    private AdminAuthService adminAuthService;
    @Mock
    private AdminPermissionService adminPermissionService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserRelationCacheService userRelationCacheService;
    @Mock
    private FriendRequestNotificationService friendRequestNotificationService;
    @Mock
    private MinioUrlResolver minioUrlResolver;

    private AdminConsoleService adminConsoleService;

    @BeforeEach
    void setUp() {
        adminConsoleService = new AdminConsoleService(
                imUserService, imSingleChatService, imGroupChatService, imGroupMemberService,
                imFriendRequestService, imFriendService, adminLoginLogService, adminOperateLogService,
                imMessageRepository, imReadStatusRepository, userChannelManager, loginService,
                adminAuthService, adminPermissionService, redisTemplate, userRelationCacheService,
                friendRequestNotificationService, minioUrlResolver
        );
    }

    @Test
    void regularAdminChatListShouldLoadLatestAndUnreadInBatch() {
        ImSingleChat chat = singleChat();
        ImUser peer = peer();
        IMMessage latest = message("chat-1", "最新消息");

        when(adminPermissionService.isSuperAdmin("user-1")).thenReturn(false);
        when(imSingleChatService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(chat));
        when(imUserService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(peer));
        when(imGroupMemberService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(imMessageRepository.findLatestByChatIds(anyList())).thenReturn(Map.of("chat-1", latest));
        when(imMessageRepository.findOtherUserMessageIds("chat-1", "user-1"))
                .thenReturn(List.of("message-1", "message-2", "message-3"));
        when(imReadStatusRepository.findReadMessageIds(
                "user-1", List.of("message-1", "message-2", "message-3")))
                .thenReturn(Set.of());

        List<ChatInfoVO> result = adminConsoleService.listChats("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastMessage()).isEqualTo("最新消息");
        assertThat(result.get(0).getUnreadCount()).isEqualTo(3);
        verify(imMessageRepository).findLatestByChatIds(argThat(chatIds -> chatIds.equals(List.of("chat-1"))));
        verify(imMessageRepository).findOtherUserMessageIds("chat-1", "user-1");
        verify(imReadStatusRepository).findReadMessageIds(
                "user-1", List.of("message-1", "message-2", "message-3"));
        verify(imMessageRepository, never()).findHistory(anyString(), any(), anyInt());
    }

    @Test
    void superAdminChatListShouldKeepUnreadAtZeroWithoutUnreadQuery() {
        ImSingleChat chat = singleChat();
        IMMessage latest = message("chat-1", "后台查看");

        when(adminPermissionService.isSuperAdmin("admin-1")).thenReturn(true);
        when(imSingleChatService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(chat));
        when(imUserService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(peer()));
        when(imGroupChatService.list(any(LambdaQueryWrapper.class))).thenReturn(List.<ImGroupChat>of());
        when(imMessageRepository.findLatestByChatIds(anyList())).thenReturn(Map.of("chat-1", latest));

        List<ChatInfoVO> result = adminConsoleService.listChats("admin-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUnreadCount()).isZero();
        verify(imMessageRepository).findLatestByChatIds(List.of("chat-1"));
        verify(imMessageRepository, never()).findOtherUserMessageIds(anyString(), anyString());
        verify(imReadStatusRepository, never()).findReadMessageIds(anyString(), anyList());
        verify(imMessageRepository, never()).findHistory(anyString(), any(), anyInt());
    }

    private ImSingleChat singleChat() {
        ImSingleChat chat = new ImSingleChat();
        chat.setChatId("chat-1");
        chat.setUser1Id("user-1");
        chat.setUser2Id("user-2");
        return chat;
    }

    private ImUser peer() {
        ImUser peer = new ImUser();
        peer.setId("user-2");
        peer.setUsername("好友");
        return peer;
    }

    private IMMessage message(String chatId, String text) {
        IMMessage message = new IMMessage();
        message.setChatId(chatId);
        message.setMessageText(text);
        message.setMessageType("TEXT");
        message.setServerTime(Instant.parse("2026-09-12T01:00:00Z"));
        return message;
    }
}
