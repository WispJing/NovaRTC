package io.github.xiaotang.novartc.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.xiaotang.novartc.server.dto.FriendRequestCreateRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestHandleRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.entity.mysql.ImFriendRequest;

import java.util.List;

/**
 * 好友申请服务，负责好友申请的查询、发起和处理。
 */
public interface IImFriendRequestService extends IService<ImFriendRequest> {
    /**
     * 查询当前用户相关的好友申请列表。
     */
    List<FriendRequestVO> listByUserId(String currentUserId);

    /**
     * 发起新的好友申请。
     */
    FriendRequestVO sendRequest(String currentUserId, FriendRequestCreateRequest request);

    /**
     * 处理指定好友申请。
     */
    FriendRequestVO handleRequest(String currentUserId, FriendRequestHandleRequest request);
}
