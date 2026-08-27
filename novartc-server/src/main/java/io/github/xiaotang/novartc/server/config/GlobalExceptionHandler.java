package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.Result;
import io.github.xiaotang.novartc.common.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    /**
     * 上传文件大小超出 multipart 限制，异常在进入控制器前抛出，需手动返回 Result
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logger.warn("上传文件大小超出限制: {}", e.getMessage());
        return Result.of(ResultCode.PARAM_ERROR.code(), "文件大小超出限制", null);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        logger.error("系统异常: {}", e.getMessage(), e);
        return Result.fail(e.getMessage());
    }
}
