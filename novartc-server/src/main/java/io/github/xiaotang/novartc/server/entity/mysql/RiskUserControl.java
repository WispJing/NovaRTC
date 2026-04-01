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
 * 风控用户控制表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("risk_user_control")
public class RiskUserControl extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 控制类型：LOGIN_BAN/SEND_MUTE
     */
    @TableField("control_type")
    private String controlType;

    /**
     * 作用域：IM/ADMIN/ALL
     */
    @TableField("scope")
    private String scope;

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
