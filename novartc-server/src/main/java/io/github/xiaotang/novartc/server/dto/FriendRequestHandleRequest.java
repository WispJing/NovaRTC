package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/25
 *
 * @author tangj
 * @description 处理好友申请请求
 */
@Data
public class FriendRequestHandleRequest {
    private String requestId;
    private String status;
}
