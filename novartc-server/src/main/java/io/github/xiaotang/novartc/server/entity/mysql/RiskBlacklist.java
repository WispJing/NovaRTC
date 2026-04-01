package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 风控黑名单表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("risk_blacklist")
public class RiskBlacklist extends BaseEntity {

    /**
     * 黑名单所属用户ID
     */
    @TableField("owner_user_id")
    private String ownerUserId;

    /**
     * 被拉黑用户ID
     */
    @TableField("blocked_user_id")
    private String blockedUserId;

    /**
     * 来源：MANUAL/AUTO
     */
    @TableField("source")
    private String source;

    /**
     * 原因
     */
    @TableField("reason")
    private String reason;

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
