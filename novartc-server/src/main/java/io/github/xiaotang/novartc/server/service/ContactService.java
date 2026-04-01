package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ContactVO;
import io.github.xiaotang.novartc.server.dto.FriendRequestCreateRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestHandleRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.dto.StartChatRequest;

import java.util.List;

/**
 * 联系人服务，负责好友列表、单聊入口和好友申请处理。
 */
public interface ContactService {
    /**
     * 查询当前用户的联系人列表。
     */
    List<ContactVO> listContacts(String currentUserId);

    /**
     * 为当前用户与目标用户创建或获取单聊会话。
     */
    ChatInfoVO startChat(String currentUserId, StartChatRequest request);

    /**
     * 查询当前用户相关的好友申请列表。
     */
    List<FriendRequestVO> listFriendRequests(String currentUserId);

    /**
     * 发起新的好友申请。
     */
    FriendRequestVO sendFriendRequest(String currentUserId, FriendRequestCreateRequest request);

    /**
     * 处理好友申请的同意、拒绝或取消操作。
     */
    FriendRequestVO handleFriendRequest(String currentUserId, FriendRequestHandleRequest request);
}
