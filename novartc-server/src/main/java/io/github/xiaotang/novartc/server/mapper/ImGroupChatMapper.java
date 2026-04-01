package io.github.xiaotang.novartc.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群聊 Mapper，负责群聊信息表的数据访问。
 */
public interface ImGroupChatMapper extends BaseMapper<ImGroupChat> {
    /**
     * 查询指定用户参与的群聊列表。
     */
    List<ImGroupChat> selectGroupChatsByUserId(@Param("userId") String userId);
}
