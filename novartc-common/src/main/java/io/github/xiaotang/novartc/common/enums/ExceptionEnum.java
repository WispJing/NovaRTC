package io.github.xiaotang.novartc.common.enums;

import lombok.Getter;

/**
 * @author Tang_J on 2025/3/2 3:02
 * @Description TODO
 **/
@Getter
public enum ExceptionEnum {
    //服务相关
    SERVER_ERROR(300, "服务器异常，请联系管理员"),
    SERVER_API_ERROR(301, "服务接口错误，请稍后重试"),
    SERVER_INVALID_URI(302, "无效的请求地址"),

    USERNAME_IS_EXIST(500, "该用户名已存在"),
    USERNAME_OR_PASSWORD_FAIL(501, "用户名或密码错误"),
    USER_UN_LOGIN(502, "用户未登录"),
    USER_ROLE_FAIL(503, "用户角色异常"),
    USER_PERMISSION_FAIL(504, "用户权限异常"),
    USER_DISABLED(505, "账号被封禁"),
    USER_UN_REQUEST_AUTH(506, "用户没有访问权限"),

    USER_TOKEN_ERROR(507, "Token无效"),
    USER_PHONE_FORMAT_ERROR(508, "手机号码格式错误"),
    USER_REGISTER_ERROR(509, "注册失败,稍后重试"),
    USER_CHECK_CODE_EXPIRE(510, "短信验证码已过期，请重新获取"),
    USER_CHECK_CODE_ERROR(511, "短信验证码错误，请重新输入"),
    USER_CHECK_CODE_SEND_ERROR(512, "短信验证码发送错误，参数传递错误"),
    USER_CHECK_CODE_SEND_FAIL(513, "短信验证码发送失败，服务端异常"),
    USER_PHONE_IS_EXIST(514, "手机号已经注册过"),
    USER_PASSWORD_IS_ERROR(515, "登录密码错误，请重新输入"),
    USER_PHONE_IS_ERROR(516, "手机号未注册，请注册后再登录"),
    USER_TEMP_TOKEN_CREATE_FAIL(517, "临时Token创建失败"),
    USER_LOGIN_EXCEPTION(518, "登录异常，请联系管理员"),
    CLIENT_ID_IS_NULL(519, "公司ID为空"),
    USER_PHONE_IS_SEAL(520, "您的手机号无法进行注册使用"),
    USER_PHONE_ADMIN_STOP(521, "您的手机号被管理员停止使用，请联系管理员"),
    CAN_NOT_MODITY_ADMIN(522, "无法更改超级管理员状态"),

    //请求参数
    INVALID_PARAM_ERROR(601, "无效的请求参数"),
    INVALID_NOTIFY_PARAM(603, "回调参数有误"),
    INVALID_NOTIFY_SIGN(604, "回调签名有误"),
    FDFS_FILE_DELETE_FAIL(605, "FDFS文件删除失败"),
    UN_FIND_CAN_UPDATE_VERSION(606, "未发现可更新APK版本"),

    //MinIO相关
    UN_SUPPORT_FILE_TYPE(900, "不支持的文件格式。请确认格式,重新上传"),
    CREATE_BUCKET_FAILED(901, "Bucket创建失败"),
    REMOVE_BUCKET_FAILED(902, "移除Bucket失败"),
    REMOVE_OBJECT_FAILED(903, "移除Object失败"),
    REMOVE_LIST_OBJECT_FAILED(904, "移除List Object失败"),

    END(999999, "");

    private int value;
    private String content;

    ExceptionEnum(int value, String content) {
        this.value = value;
        this.content = content;
    }
}
