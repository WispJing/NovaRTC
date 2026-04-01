package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
@TableName("risk_group_mute")
public class RiskGroupMute extends BaseEntity {
    @TableField("group_id")
    private String groupId;

    @TableField("user_id")
    private String userId;

    @TableField("mute_scope")
    private String muteScope;

    @TableField("reason")
    private String reason;

    @TableField("expires_at")
    private Instant expiresAt;

    @TableField("status")
    private Byte status;

    @TableField("operator_user_id")
    private String operatorUserId;

    @TableField("operator_name")
    private String operatorName;
}
