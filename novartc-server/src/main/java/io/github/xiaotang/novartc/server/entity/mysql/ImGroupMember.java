package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * <p>
 * 群成员关系表
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-18
 */
@Getter
@Setter
@ToString
@TableName("im_group_member")
public class ImGroupMember extends BaseEntity {

    /**
     * 群聊ID
     */
    @TableField("group_id")
    private String groupId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 加入时间
     */
    @TableField("joined_at")
    private LocalDateTime joinedAt;
}
