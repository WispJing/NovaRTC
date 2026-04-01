package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 后台操作日志表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("admin_operate_log")
public class AdminOperateLog extends BaseEntity {

    /**
     * 操作人用户ID
     */
    @TableField("operator_user_id")
    private String operatorUserId;

    /**
     * 操作人用户编号
     */
    @TableField("operator_user_num")
    private String operatorUserNum;

    /**
     * 操作人名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 模块
     */
    @TableField("module")
    private String module;

    /**
     * 动作
     */
    @TableField("action")
    private String action;

    /**
     * 目标对象类型
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标对象ID
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 请求方法
     */
    @TableField("request_method")
    private String requestMethod;

    /**
     * 请求路径
     */
    @TableField("request_uri")
    private String requestUri;

    /**
     * 请求参数
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 结果码
     */
    @TableField("result_code")
    private String resultCode;

    /**
     * 结果消息
     */
    @TableField("result_msg")
    private String resultMsg;

    /**
     * 是否成功：0-否，1-是
     */
    @TableField("success")
    private Byte success;

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
