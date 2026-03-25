package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.ChatInfoVO;
import io.github.xiaotang.novartc.server.dto.ContactVO;
import io.github.xiaotang.novartc.server.dto.FriendRequestCreateRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestHandleRequest;
import io.github.xiaotang.novartc.server.dto.FriendRequestVO;
import io.github.xiaotang.novartc.server.dto.StartChatRequest;

import java.util.List;

public interface ContactService {
    List<ContactVO> listContacts(String currentUserId);

    ChatInfoVO startChat(String currentUserId, StartChatRequest request);

    List<FriendRequestVO> listFriendRequests(String currentUserId);

    FriendRequestVO sendFriendRequest(String currentUserId, FriendRequestCreateRequest request);

    FriendRequestVO handleFriendRequest(String currentUserId, FriendRequestHandleRequest request);
}
