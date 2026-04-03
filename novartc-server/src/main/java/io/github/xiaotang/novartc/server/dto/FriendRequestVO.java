package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

import java.time.Instant;

/**
 * Created on 2026/3/25
 *
 * @author tangj
 * @description 好友申请视图对象
 */
@Data
public class FriendRequestVO {
    private String requestId;
    private String requesterId;
    private String requesterUserNum;
    private String requesterUsername;
    private String requesterAvatarUrl;
    private String receiverId;
    private String receiverUserNum;
    private String receiverUsername;
    private String receiverAvatarUrl;
    private String status;
    private String requestMsg;
    private Instant handledAt;
    private String handlerId;
    private Instant createdAt;
    private Instant updatedAt;
}
