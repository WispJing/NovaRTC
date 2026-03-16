package io.github.xiaotang.novartc.common.exception;

import io.github.xiaotang.novartc.common.result.Result;
import lombok.Getter;

/**
 * Created on 2026/3/16 16:12
 *
 * @author tangj
 * @description
 */
@Getter
public class ApplicationException extends RuntimeException {
    private final int code;
    private Object data;

    public ApplicationException(String message) {
        super(message, null, true, true);
        this.code = Result.RECODE_VALIDATE_ERROR;
    }

    public ApplicationException(int code, String message) {
        super(message, null, true, true);
        this.code = code;
    }

    public ApplicationException(String message, Object data) {
        super(message, null, true, true);
        this.code = Result.RECODE_VALIDATE_ERROR;
        this.data = data;
    }

    public ApplicationException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }
}
