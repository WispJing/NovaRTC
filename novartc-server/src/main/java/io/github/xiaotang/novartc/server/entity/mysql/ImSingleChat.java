package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 单聊信息表
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
@Getter
@Setter
@ToString
@TableName("im_single_chat")
public class ImSingleChat extends BaseEntity {

    /**
     * 聊天会话ID（UUID32）
     */
    @TableField("chat_id")
    private String chatId;

    /**
     * 用户1ID
     */
    @TableField("user1_id")
    private String user1Id;

    /**
     * 用户2ID
     */
    @TableField("user2_id")
    private String user2Id;
}
