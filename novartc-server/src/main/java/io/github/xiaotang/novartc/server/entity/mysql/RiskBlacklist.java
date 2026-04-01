package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("risk_blacklist")
public class RiskBlacklist extends BaseEntity {
    @TableField("owner_user_id")
    private String ownerUserId;

    @TableField("blocked_user_id")
    private String blockedUserId;

    @TableField("source")
    private String source;

    @TableField("reason")
    private String reason;

    @TableField("status")
    private Byte status;

    @TableField("operator_user_id")
    private String operatorUserId;

    @TableField("operator_name")
    private String operatorName;
}
