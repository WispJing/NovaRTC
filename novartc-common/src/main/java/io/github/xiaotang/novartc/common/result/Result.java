package io.github.xiaotang.novartc.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * Created on 2026/3/11 11:10
 *
 * @author tangj
 * @description
 */
@Data
public class Result<T> implements Serializable {

    /**
     * 状态码
     */
    private int code;

    /**
     * 返回消息
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    public Result() {
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功返回
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    /**
     * 成功返回（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(0, "success", null);
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(-1, msg, null);
    }

    /**
     * 自定义返回
     */
    public static <T> Result<T> of(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

}
