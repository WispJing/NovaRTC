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
@TableName("risk_user_control")
public class RiskUserControl extends BaseEntity {
    @TableField("user_id")
    private String userId;

    @TableField("control_type")
    private String controlType;

    @TableField("scope")
    private String scope;

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
