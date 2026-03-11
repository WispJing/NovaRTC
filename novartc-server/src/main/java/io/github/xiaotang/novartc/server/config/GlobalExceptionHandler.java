package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Created on 2026/3/11 11:16
 *
 * @author tangj
 * @description
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.of(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.fail(e.getMessage());
    }
}
