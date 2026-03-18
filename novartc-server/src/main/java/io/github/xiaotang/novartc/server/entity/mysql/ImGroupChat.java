package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 群聊信息表
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
@Getter
@Setter
@ToString
@TableName("im_group_chat")
public class ImGroupChat extends BaseEntity {

    /**
     * 聊天会话ID（UUID32）
     */
    @TableField("chat_id")
    private String chatId;

    /**
     * 群号（对外展示）
     */
    @TableField("group_num")
    private String groupNum;

    /**
     * 群名称
     */
    @TableField("group_name")
    private String groupName;
}
