package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 好友关系表
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-25
 */
@Getter
@Setter
@ToString
@TableName("im_friend")
public class ImFriend extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 好友用户ID
     */
    @TableField("friend_id")
    private String friendId;
}
