package io.github.xiaotang.novartc.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupMember;
import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.repository.mongodb.IMReadStatusRepository;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImGroupMemberService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.util.MinioUrlResolver;
import io.github.xiaotang.novartc.server.util.MinioUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceListQueryTest {

    @Mock
    private IImUserService imUserService;
    @Mock
    private IImSingleChatService imSingleChatService;
    @Mock
    private IImGroupChatService imGroupChatService;
    @Mock
    private IImGroupMemberService imGroupMemberService;
    @Mock
    private IMMessageRepository imMessageRepository;
    @Mock
    private IMReadStatusRepository imReadStatusRepository;
    @Mock
    private UserRouteService userRouteService;
    @Mock
    private ChatDispatchService chatDispatchService;
    @Mock
    private ImFileMapper imFileMapper;
    @Mock
    private MinioUtil minioUtil;
    @Mock
    private MinioUrlResolver minioUrlResolver;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId("user-1");
        chatService = new ChatServiceImpl(
                imUserService, imSingleChatService, imGroupChatService, imGroupMemberService,
                imMessageRepository, imReadStatusRepository, userRouteService, chatDispatchService,
                imFileMapper, minioUtil, new MinioProperties(), minioUrlResolver
        );
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void chatListShouldLoadSummariesInBatchAndKeepResultSemantics() {
        ImSingleChat singleChat = new ImSingleChat();
        singleChat.setChatId("chat-single");
        singleChat.setUser1Id("user-1");
        singleChat.setUser2Id("user-2");
        ImUser peer = new ImUser();
        peer.setId("user-2");
        peer.setUsername("好友");
        peer.setUserNum("10002");

        ImGroupMember membership = new ImGroupMember();
        membership.setGroupId("group-1");
        membership.setUserId("user-1");
        ImGroupChat groupChat = new ImGroupChat();
        groupChat.setId("group-1");
        groupChat.setChatId("chat-group");
        groupChat.setGroupNum("20001");
        groupChat.setGroupName("测试群");

        IMMessage singleLatest = message("chat-single", "你好", "TEXT", Instant.parse("2026-09-12T01:00:00Z"));
        IMMessage groupLatest = message("chat-group", null, "IMAGE", null);
        groupLatest.setClientTime(Instant.parse("2026-09-12T02:00:00Z"));

        when(imSingleChatService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(singleChat));
        when(imUserService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(peer));
        when(imGroupMemberService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(membership), List.of(membership));
        when(imGroupChatService.list(any(LambdaQueryWrapper.class))).thenReturn(List.of(groupChat));
        when(userRouteService.getUserInstances(anyString())).thenReturn(Set.of());
        when(imMessageRepository.findLatestByChatIds(anyList()))
                .thenReturn(Map.of("chat-single", singleLatest, "chat-group", groupLatest));
        when(imMessageRepository.findOtherUserMessageIdsByChatIds(anyList(), anyString()))
                .thenReturn(Map.of(
                        "chat-single", List.of("single-read", "single-unread"),
                        "chat-group", List.of("group-read", "group-unread-1", "group-unread-2")
                ));
        when(imReadStatusRepository.findReadMessageIds(eq("user-1"), anyList()))
                .thenReturn(Set.of("single-read", "group-read"));

        List<ChatInfoVO> result = chatService.chatList();

        assertThat(result).extracting(ChatInfoVO::getChatId)
                .containsExactly("chat-group", "chat-single");
        assertThat(result.get(0).getLastMessage()).isEqualTo("[图片]");
        assertThat(result.get(0).getLastMessageTime()).isEqualTo(groupLatest.getClientTime());
        assertThat(result.get(0).getUnreadCount()).isEqualTo(2);
        assertThat(result.get(1).getLastMessage()).isEqualTo("你好");
        assertThat(result.get(1).getUnreadCount()).isEqualTo(1);

        verify(imMessageRepository).findLatestByChatIds(argThat(chatIds ->
                chatIds.size() == 2 && chatIds.containsAll(List.of("chat-single", "chat-group"))));
        verify(imMessageRepository).findOtherUserMessageIdsByChatIds(argThat(chatIds ->
                chatIds.size() == 2 && chatIds.containsAll(List.of("chat-single", "chat-group"))), eq("user-1"));
        verify(imReadStatusRepository).findReadMessageIds(
                eq("user-1"), argThat(messageIds -> messageIds.size() == 5 && messageIds.containsAll(List.of(
                        "single-read", "single-unread", "group-read", "group-unread-1", "group-unread-2"))));
        verify(imMessageRepository, never()).findHistory(anyString(), any(), anyInt());
    }

    private IMMessage message(String chatId, String text, String type, Instant serverTime) {
        IMMessage message = new IMMessage();
        message.setChatId(chatId);
        message.setMessageText(text);
        message.setMessageType(type);
        message.setServerTime(serverTime);
        return message;
    }
}
