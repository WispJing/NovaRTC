package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 群聊信息表 服务类
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
public interface IImGroupChatService extends IService<ImGroupChat> {

    List<ImGroupChat> getByUserId(String userId);
}
