package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.server.Interceptor.LogInterceptor;
import io.github.xiaotang.novartc.server.Interceptor.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Created on 2026/3/19 15:21
 *
 * @author tangj
 * @description
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final LogInterceptor logInterceptor;
    private final TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 日志拦截器（对所有路径生效）
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");

        // 2. Token 拦截器：排除登录接口、静态资源等
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/login",
                        "/api/register",
                        "/druid/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}
