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
 * 风控登录告警表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("risk_login_alert")
public class RiskLoginAlert extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 用户编号
     */
    @TableField("user_num")
    private String userNum;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 客户端类型
     */
    @TableField("client_type")
    private String clientType;

    /**
     * IP地址
     */
    @TableField("ip")
    private String ip;

    /**
     * 用户代理
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 告警类型：LOGIN_BANNED/PASSWORD_ERROR/MULTI_CLIENT
     */
    @TableField("alert_type")
    private String alertType;

    /**
     * 告警等级：LOW/MEDIUM/HIGH
     */
    @TableField("alert_level")
    private String alertLevel;

    /**
     * 告警详情
     */
    @TableField("detail")
    private String detail;

    /**
     * 处理状态：0-未处理，1-已处理
     */
    @TableField("handled_status")
    private Byte handledStatus;

    /**
     * 处理人ID
     */
    @TableField("handler_user_id")
    private String handlerUserId;

    /**
     * 处理人名称
     */
    @TableField("handler_name")
    private String handlerName;

    /**
     * 处理时间
     */
    @TableField("handled_at")
    private LocalDateTime handledAt;
}
