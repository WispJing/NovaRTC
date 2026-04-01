package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 单聊会话服务，负责单聊会话查询与创建。
 */
public interface IImSingleChatService extends IService<ImSingleChat> {
    /**
     * 查询指定用户参与的单聊会话。
     */
    List<ImSingleChat> getByUserId(String userId);

    /**
     * 按双方用户标识查询单聊会话。
     */
    ImSingleChat getByUsers(String user1Id, String user2Id);

    /**
     * 创建单聊会话，若已存在则直接返回。
     */
    ImSingleChat createOrGet(String currentUserId, String targetUserId);
}
