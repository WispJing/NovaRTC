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
 * 后台登录日志表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("admin_login_log")
public class AdminLoginLog extends BaseEntity {

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
     * 登录类型：ADMIN/IM
     */
    @TableField("login_type")
    private String loginType;

    /**
     * 客户端类型
     */
    @TableField("client_type")
    private String clientType;

    /**
     * 登录token
     */
    @TableField("token")
    private String token;

    /**
     * 登录时间
     */
    @TableField("login_time")
    private LocalDateTime loginTime;

    /**
     * 登出时间
     */
    @TableField("logout_time")
    private LocalDateTime logoutTime;

    /**
     * 登录状态：0-失败，1-成功，2-已踢下线，3-已登出
     */
    @TableField("status")
    private Byte status;

    /**
     * 失败原因
     */
    @TableField("fail_reason")
    private String failReason;

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
}
