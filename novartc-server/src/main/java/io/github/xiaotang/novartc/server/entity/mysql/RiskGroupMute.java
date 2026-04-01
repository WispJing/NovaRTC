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
 * 风控群禁言表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("risk_group_mute")
public class RiskGroupMute extends BaseEntity {

    /**
     * 群组ID
     */
    @TableField("group_id")
    private String groupId;

    /**
     * 用户ID，空表示全群禁言
     */
    @TableField("user_id")
    private String userId;

    /**
     * 禁言范围：GROUP_ALL/USER
     */
    @TableField("mute_scope")
    private String muteScope;

    /**
     * 原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 失效时间，空表示永久
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * 状态：0-关闭，1-生效
     */
    @TableField("status")
    private Byte status;

    /**
     * 操作人ID
     */
    @TableField("operator_user_id")
    private String operatorUserId;

    /**
     * 操作人名称
     */
    @TableField("operator_name")
    private String operatorName;
}
