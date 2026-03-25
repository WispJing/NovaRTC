package io.github.xiaotang.novartc.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.xiaotang.novartc.server.dto.FriendRequestCreateRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestHandleRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriendRequest;

import java.util.List;

public interface IImFriendRequestService extends IService<ImFriendRequest> {
    List<FriendRequestVO> listByUserId(String currentUserId);

    FriendRequestVO sendRequest(String currentUserId, FriendRequestCreateRequest request);

    FriendRequestVO handleRequest(String currentUserId, FriendRequestHandleRequest request);
}
