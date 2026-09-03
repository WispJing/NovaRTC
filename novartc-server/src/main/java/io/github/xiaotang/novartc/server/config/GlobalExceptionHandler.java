package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.Result;
import io.github.xiaotang.novartc.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Set;

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

    /**
     * 请求路径存在但 HTTP 方法不匹配。该异常发生在 Spring 路由匹配阶段，
     * 因此堆栈中不会出现业务 Controller 的代码位置。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<?> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        Set<?> supportedMethods = e.getSupportedHttpMethods();
        logger.warn("请求方法不支持: method={}, uri={}, query={}, supportedMethods={}",
                request.getMethod(), request.getRequestURI(), request.getQueryString(), supportedMethods);
        return Result.of(ResultCode.PARAM_ERROR.code(),
                "请求方法不支持，请使用: " + supportedMethods, null);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        logger.error("系统异常: method={}, uri={}, query={}, message={}",
                request.getMethod(), request.getRequestURI(), request.getQueryString(), e.getMessage(), e);
        return Result.fail(e.getMessage());
    }
}
