package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 群聊服务，负责群会话信息查询与基础数据操作。
 */
public interface IImGroupChatService extends IService<ImGroupChat> {
    /**
     * 查询指定用户参与的群聊列表。
     */
    List<ImGroupChat> getByUserId(String userId);
}
