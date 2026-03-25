package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.server.entity.mysql.ImFriend;
import io.github.xiaotang.novartc.server.mapper.ImFriendMapper;
import io.github.xiaotang.novartc.server.service.IImFriendService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 好友关系表 服务实现类
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-25
 */
@Service
public class ImFriendServiceImpl extends ServiceImpl<ImFriendMapper, ImFriend> implements IImFriendService {

}
