package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.entity.mysql.ImSingleChat;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 单聊信息表 服务类
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
public interface IImSingleChatService extends IService<ImSingleChat> {

    List<ImSingleChat> getByUserId(String userId);

    ImSingleChat getByUsers(String user1Id, String user2Id);

    ImSingleChat createOrGet(String currentUserId, String targetUserId);
}
