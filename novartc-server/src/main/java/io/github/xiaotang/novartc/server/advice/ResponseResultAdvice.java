package io.github.xiaotang.novartc.server.advice;

import io.github.xiaotang.novartc.common.annotation.IgnoreResponseAdvice;
import io.github.xiaotang.novartc.common.result.Result;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Created on 2026/3/11 11:12
 *
 * @author tangj
 * @description
 */
@RestControllerAdvice
public class ResponseResultAdvice implements ResponseBodyAdvice<Object> {
    /**
     * 是否需要包装
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {

        // 已经是 Result 类型就不再包装
        return !returnType.hasMethodAnnotation(IgnoreResponseAdvice.class)
                && !returnType.getParameterType().equals(Result.class);
    }

    /**
     * 返回值处理
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // String 类型需要特殊处理
        if (body instanceof String) {
            try {
                return JacksonUtil.getMapper().writeValueAsString(Result.success(body));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 已经是 Result 不处理
        if (body instanceof Result) {
            return body;
        }

        // 自动包装
        return Result.success(body);
    }
}
