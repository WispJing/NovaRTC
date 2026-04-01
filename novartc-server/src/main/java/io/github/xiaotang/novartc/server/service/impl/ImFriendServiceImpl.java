package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.ImFriend;
import io.github.xiaotang.novartc.server.mapper.ImFriendMapper;
import io.github.xiaotang.novartc.server.service.IImFriendService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 好友关系服务实现，提供好友关系的基础数据操作能力。
 */
@Service
public class ImFriendServiceImpl extends ServiceImpl<ImFriendMapper, ImFriend> implements IImFriendService {

}
