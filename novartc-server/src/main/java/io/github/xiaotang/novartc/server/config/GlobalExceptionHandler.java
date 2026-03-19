package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage(), e);
        return Result.of(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(ApplicationException.class)
    public Result<?> handleApplicationException(ApplicationException e) {
        logger.error("应用异常: {}", e.getMessage(), e);
        return Result.of(e.getCode(), e.getMessage(), e.getData());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        logger.error("系统异常: {}", e.getMessage(), e);
        return Result.fail(e.getMessage());
    }
}
