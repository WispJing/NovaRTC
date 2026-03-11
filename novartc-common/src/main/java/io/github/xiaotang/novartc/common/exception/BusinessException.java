package io.github.xiaotang.novartc.common.exception;

import io.github.xiaotang.novartc.common.result.ResultCode;
import lombok.Getter;

/**
 * Created on 2026/3/11 11:30
 *
 * @author tangj
 * @description
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String message;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.msg());
        this.code = resultCode.code();
        this.message = resultCode.msg();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
