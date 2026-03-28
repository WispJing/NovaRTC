package io.github.xiaotang.novartc.server.advice;

import io.github.xiaotang.novartc.common.annotation.IgnoreResponseAdvice;
import io.github.xiaotang.novartc.common.result.Result;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
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
    private static final String RESPONSE_BODY_ATTR = "responseBody";
    private static final String ACTUATOR_PACKAGE_PREFIX = "org.springframework.boot.actuate";

    /**
     * 是否需要包装
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> containingClass = returnType.getContainingClass();
        if (containingClass.getName().startsWith(ACTUATOR_PACKAGE_PREFIX)) {
            return false;
        }

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

        if (body instanceof String) {
            try {
                String wrappedBody = JacksonUtil.getMapper().writeValueAsString(Result.success(body));
                saveResponseBody(request, wrappedBody);
                return wrappedBody;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (body instanceof Result) {
            saveResponseBody(request, body);
            return body;
        }

        Object wrappedBody = Result.success(body);
        saveResponseBody(request, wrappedBody);
        return wrappedBody;
    }

    private void saveResponseBody(ServerHttpRequest request, Object body) {
        if (request instanceof ServletServerHttpRequest servletServerHttpRequest) {
            HttpServletRequest servletRequest = servletServerHttpRequest.getServletRequest();
            servletRequest.setAttribute(RESPONSE_BODY_ATTR, body);
        }
    }
}
