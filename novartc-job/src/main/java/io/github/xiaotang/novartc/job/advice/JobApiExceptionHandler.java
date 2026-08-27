package io.github.xiaotang.novartc.job.advice;

import io.github.xiaotang.novartc.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 接口统一异常处理：业务校验与参数校验失败返回可读错误信息。
 *
 * @author xiaotang
 */
@RestControllerAdvice
public class JobApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(JobApiExceptionHandler.class);

    /**
     * 业务校验失败
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(e.getMessage());
    }

    /**
     * 调度器/数据库等服务异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException e) {
        logger.error("调度平台服务异常", e);
        return Result.fail(e.getMessage());
    }

    /**
     * 请求体参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(message);
    }
}
