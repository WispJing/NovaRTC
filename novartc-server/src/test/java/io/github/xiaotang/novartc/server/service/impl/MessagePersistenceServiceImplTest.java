package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.util.MessageIdUtil;
import io.github.xiaotang.novartc.server.entity.mongodb.IMMessage;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.repository.mongodb.IMMessageRepository;
import io.github.xiaotang.novartc.server.service.ChatSeqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息持久化单元测试：验证文件/视频/语音内容写入 contentJson。
 */
@ExtendWith(MockitoExtension.class)
class MessagePersistenceServiceImplTest {

    @Mock
    private IMMessageRepository imMessageRepository;

    @Mock
    private ChatSeqService chatSeqService;

    @Mock
    private MessageIdUtil messageIdUtil;

    private MessagePersistenceServiceImpl persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new MessagePersistenceServiceImpl(
                imMessageRepository, chatSeqService, messageIdUtil);
        when(chatSeqService.nextSeq("chat-1")).thenReturn(10L);
        when(messageIdUtil.nextId()).thenReturn(1L);
    }

    private ChatMessage.MsgBody.Builder baseSingle(ChatMessage.MessageType type) {
        return ChatMessage.MsgBody.newBuilder()
                .setMessageType(type)
                .setIsGroup(false)
                .setSingleChatInfo(ChatMessage.SingleChatInfo.newBuilder()
                        .setChatId("chat-1")
                        .setUser1Id("U1001")
                        .setUser2Id("U1002")
                        .build())
                .setSender(ChatMessage.User.newBuilder()
                        .setUserId("U1001")
                        .setUsername("tester")
                        .build());
    }

    @Test
    void persistShouldStoreFileContent() {
        ChatMessage.MsgBody message = baseSingle(ChatMessage.MessageType.FILE)
                .setFileContent(ChatMessage.FileContent.newBuilder()
                        .setFileUrl("http://localhost:9000/novartc/file/a.pdf")
                        .setFileName("a.pdf")
                        .setFileSize(1024)
                        .setContentType("application/pdf")
                        .build())
                .build();

        persistenceService.persist(message, "U1001");

        ArgumentCaptor<IMMessage> captor = ArgumentCaptor.forClass(IMMessage.class);
        verify(imMessageRepository).insert(captor.capture());
        IMMessage entity = captor.getValue();
        assertThat(entity.getMessageType()).isEqualTo("FILE");
        assertThat(entity.getContentJson())
                .contains("\"fileUrl\":\"http://localhost:9000/novartc/file/a.pdf\"")
                .contains("\"fileName\":\"a.pdf\"")
                .contains("\"contentType\":\"application/pdf\"");
    }

    @Test
    void persistShouldStoreVideoContent() {
        ChatMessage.MsgBody message = baseSingle(ChatMessage.MessageType.VIDEO)
                .setVideoContent(ChatMessage.VideoContent.newBuilder()
                        .setVideoUrl("http://localhost:9000/novartc/video/b.mp4")
                        .setFileName("b.mp4")
                        .setFileSize(2048)
                        .build())
                .build();

        persistenceService.persist(message, "U1001");

        ArgumentCaptor<IMMessage> captor = ArgumentCaptor.forClass(IMMessage.class);
        verify(imMessageRepository).insert(captor.capture());
        IMMessage entity = captor.getValue();
        assertThat(entity.getMessageType()).isEqualTo("VIDEO");
        assertThat(entity.getContentJson())
                .contains("\"videoUrl\":\"http://localhost:9000/novartc/video/b.mp4\"")
                .contains("\"fileName\":\"b.mp4\"");
    }

    @Test
    void persistShouldStoreAudioContentWithDuration() {
        ChatMessage.MsgBody message = baseSingle(ChatMessage.MessageType.AUDIO)
                .setAudioContent(ChatMessage.AudioContent.newBuilder()
                        .setAudioUrl("http://localhost:9000/novartc/audio/c.wav")
                        .setFileName("c.wav")
                        .setFileSize(512)
                        .setDurationSeconds(9)
                        .build())
                .build();

        persistenceService.persist(message, "U1001");

        ArgumentCaptor<IMMessage> captor = ArgumentCaptor.forClass(IMMessage.class);
        verify(imMessageRepository).insert(captor.capture());
        IMMessage entity = captor.getValue();
        assertThat(entity.getMessageType()).isEqualTo("AUDIO");
        assertThat(entity.getContentJson())
                .contains("\"audioUrl\":\"http://localhost:9000/novartc/audio/c.wav\"")
                .contains("\"durationSeconds\":9");
    }
}
