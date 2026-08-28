package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.repository.mongodb.IMReadStatusRepository;
import io.github.xiaotang.novartc.server.service.IImGroupChatService;
import io.github.xiaotang.novartc.server.service.IImGroupMemberService;
import io.github.xiaotang.novartc.server.service.IImSingleChatService;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.util.MinioUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息撤回单元测试，不启动 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceRecallTest {

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

    private MinioProperties minioProperties;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId("U1001");
        minioProperties = new MinioProperties();
        minioProperties.setBucket("novartc");
        chatService = new ChatServiceImpl(
                imUserService, imSingleChatService, imGroupChatService, imGroupMemberService,
                imMessageRepository, imReadStatusRepository, userRouteService,
                chatDispatchService, imFileMapper, minioUtil, minioProperties);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    private IMMessage imageMessage(Instant serverTime) {
        IMMessage message = new IMMessage();
        message.setMessageId("msg-1");
        message.setChatId("chat-1");
        message.setSenderId("U1001");
        message.setTargetUserId("U1002");
        message.setIsGroup(Boolean.FALSE);
        message.setMessageType("IMAGE");
        message.setContentJson("{\"imageUrl\":\"http://localhost:6000/novartc/image/x.png\"}");
        message.setServerTime(serverTime);
        return message;
    }

    private ImFile matchedFile() {
        ImFile imFile = new ImFile();
        imFile.setId("file-1");
        imFile.setObjectKey("image/20260828/x.png");
        imFile.setUrl("http://localhost:6000/novartc/image/x.png");
        return imFile;
    }

    @Test
    void recallShouldDeleteFileMarkRecalledAndDispatch() {
        when(imMessageRepository.findByMessageId("msg-1"))
                .thenReturn(imageMessage(Instant.now().minusSeconds(30)));
        when(imFileMapper.selectOne(any())).thenReturn(matchedFile());

        chatService.recall("chat-1", "msg-1");

        // MinIO 对象与文件记录均被删除
        verify(minioUtil).removeObject("novartc", "image/20260828/x.png");
        verify(imFileMapper).deleteById("file-1");

        // 消息被标记撤回并保存
        ArgumentCaptor<IMMessage> messageCaptor = ArgumentCaptor.forClass(IMMessage.class);
        verify(imMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getRecalled()).isTrue();

        // 撤回通知帧下发给对端
        ArgumentCaptor<ChatMessage.MsgBody> frameCaptor = ArgumentCaptor.forClass(ChatMessage.MsgBody.class);
        verify(chatDispatchService).dispatch(frameCaptor.capture());
        ChatMessage.MsgBody frame = frameCaptor.getValue();
        assertThat(frame.getMessageType()).isEqualTo(ChatMessage.MessageType.RECALL);
        assertThat(frame.getRecallContent().getRecallMessageId()).isEqualTo("msg-1");
        assertThat(frame.getRecallContent().getOperatorId()).isEqualTo("U1001");
        assertThat(frame.getSingleChatInfo().getUser2Id()).isEqualTo("U1002");
    }

    @Test
    void recallShouldRejectMessageSentByOthers() {
        IMMessage message = imageMessage(Instant.now());
        message.setSenderId("U2002");
        when(imMessageRepository.findByMessageId("msg-1")).thenReturn(message);

        assertThatThrownBy(() -> chatService.recall("chat-1", "msg-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能撤回自己的消息");
        verify(imMessageRepository, never()).save(any());
    }

    @Test
    void recallShouldRejectAfterTwoMinutes() {
        when(imMessageRepository.findByMessageId("msg-1"))
                .thenReturn(imageMessage(Instant.now().minusSeconds(121)));

        assertThatThrownBy(() -> chatService.recall("chat-1", "msg-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已超过撤回时限");
        verify(minioUtil, never()).removeObject(any(), any());
    }

    @Test
    void recallShouldRejectAlreadyRecalledMessage() {
        IMMessage message = imageMessage(Instant.now());
        message.setRecalled(Boolean.TRUE);
        when(imMessageRepository.findByMessageId("msg-1")).thenReturn(message);

        assertThatThrownBy(() -> chatService.recall("chat-1", "msg-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息已被撤回");
    }

    @Test
    void recallShouldRejectUnknownMessage() {
        when(imMessageRepository.findByMessageId("none")).thenReturn(null);

        assertThatThrownBy(() -> chatService.recall("chat-1", "none"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息不存在");
    }

    @Test
    void recallShouldRejectChatIdMismatch() {
        when(imMessageRepository.findByMessageId("msg-1"))
                .thenReturn(imageMessage(Instant.now()));

        assertThatThrownBy(() -> chatService.recall("other-chat", "msg-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("消息不存在");
    }

    @Test
    void recallShouldSucceedWithoutFileWhenContentHasNoUrl() {
        IMMessage message = imageMessage(Instant.now());
        message.setMessageType("TEXT");
        message.setContentJson("{\"text\":\"hello\"}");
        when(imMessageRepository.findByMessageId("msg-1")).thenReturn(message);

        chatService.recall("chat-1", "msg-1");

        verify(imFileMapper, never()).selectOne(any());
        verify(minioUtil, never()).removeObject(any(), any());
        verify(imMessageRepository).save(any());
        verify(chatDispatchService).dispatch(any());
    }
}
