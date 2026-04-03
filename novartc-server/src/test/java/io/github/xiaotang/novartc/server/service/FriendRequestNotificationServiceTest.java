package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.enums.FriendRequestStatus;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriendRequest;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import io.github.xiaotang.novartc.server.queue.ChatDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendRequestNotificationServiceTest {

    @Mock
    private ChatDispatchService chatDispatchService;

    @Mock
    private IImUserService imUserService;

    @InjectMocks
    private FriendRequestNotificationService friendRequestNotificationService;

    @Test
    void shouldEmitHandledAtAndCreatedAtAsEpochMillisFromInstant() {
        ImFriendRequest request = new ImFriendRequest();
        request.setId("req-1");
        request.setRequesterId("user-a");
        request.setReceiverId("user-b");
        request.setStatus(FriendRequestStatus.ACCEPTED.name());
        request.setRequestMsg("hello");
        request.setCreatedAt(Instant.parse("2026-04-03T01:02:03Z"));
        request.setHandledAt(Instant.parse("2026-04-03T04:05:06Z"));
        request.setHandlerId("user-b");

        friendRequestNotificationService.notifyUpdated(request);

        ArgumentCaptor<ChatMessage.MsgBody> captor = ArgumentCaptor.forClass(ChatMessage.MsgBody.class);
        verify(chatDispatchService, times(2)).dispatch(captor.capture());

        ChatMessage.FriendRequestEvent event = captor.getAllValues().getFirst().getFriendRequestEvent();
        assertEquals(Instant.parse("2026-04-03T01:02:03Z").toEpochMilli(), event.getCreatedAt());
        assertEquals(Instant.parse("2026-04-03T04:05:06Z").toEpochMilli(), event.getHandledAt());
    }
}
