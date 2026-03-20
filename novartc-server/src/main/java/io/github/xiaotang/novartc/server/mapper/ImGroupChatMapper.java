package io.github.xiaotang.novartc.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.xiaotang.novartc.server.entity.mysql.ImGroupChat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 群聊信息表 Mapper 接口
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
public interface ImGroupChatMapper extends BaseMapper<ImGroupChat> {
    List<ImGroupChat> selectGroupChatsByUserId(@Param("userId") String userId);
}
