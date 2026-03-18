package io.github.xiaotang.novartc.common.result;

/**
 * Created on 2026/3/11 11:10
 *
 * @author tangj
 * @description
 */
public enum ResultCode {

    SUCCESS(0, "success"),
    FAIL(-1, "fail"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    LOGIN_FAIL(402, "未登录"),
    FORBIDDEN(403, "无权限"),
    SERVER_ERROR(500, "服务器异常");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int code() {
        return code;
    }

    public String msg() {
        return msg;
    }
}
