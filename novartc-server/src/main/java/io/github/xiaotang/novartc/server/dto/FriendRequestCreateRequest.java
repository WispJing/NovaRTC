package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/25
 *
 * @author tangj
 * @description 发起好友申请请求
 */
@Data
public class FriendRequestCreateRequest {
    private String targetUserId;
    private String requestMsg;
}
