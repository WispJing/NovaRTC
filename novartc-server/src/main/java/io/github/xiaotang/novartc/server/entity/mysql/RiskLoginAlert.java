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
@TableName("risk_login_alert")
public class RiskLoginAlert extends BaseEntity {
    @TableField("user_id")
    private String userId;

    @TableField("user_num")
    private String userNum;

    @TableField("username")
    private String username;

    @TableField("client_type")
    private String clientType;

    @TableField("ip")
    private String ip;

    @TableField("user_agent")
    private String userAgent;

    @TableField("alert_type")
    private String alertType;

    @TableField("alert_level")
    private String alertLevel;

    @TableField("detail")
    private String detail;

    @TableField("handled_status")
    private Byte handledStatus;

    @TableField("handler_user_id")
    private String handlerUserId;

    @TableField("handler_name")
    private String handlerName;

    @TableField("handled_at")
    private Instant handledAt;
}
