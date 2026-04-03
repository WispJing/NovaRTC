package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * <p>
 * 好友申请表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-03
 */
@Getter
@Setter
@ToString
@TableName("im_friend_request")
public class ImFriendRequest extends BaseEntity {

    /**
     * 申请人用户ID
     */
    @TableField("requester_id")
    private String requesterId;

    /**
     * 接收人用户ID
     */
    @TableField("receiver_id")
    private String receiverId;

    /**
     * 申请状态：PENDING/ACCEPTED/REJECTED/CANCELED
     */
    @TableField("status")
    private String status;

    /**
     * 申请附言
     */
    @TableField("request_msg")
    private String requestMsg;

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private Instant handledAt;

    /**
     * 处理人用户ID
     */
    @TableField("handler_id")
    private String handlerId;
}
