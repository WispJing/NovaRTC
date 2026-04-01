package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.server.Interceptor.AdminOperateLogInterceptor;
import io.github.xiaotang.novartc.server.Interceptor.AdminTokenInterceptor;
import io.github.xiaotang.novartc.server.Interceptor.LogInterceptor;
import io.github.xiaotang.novartc.server.Interceptor.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final LogInterceptor logInterceptor;
    private final TokenInterceptor tokenInterceptor;
    private final AdminTokenInterceptor adminTokenInterceptor;
    private final AdminOperateLogInterceptor adminOperateLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/login",
                        "/api/register",
                        "/favicon.ico",
                        "/error"
                );

        registry.addInterceptor(adminTokenInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/auth/login",
                        "/favicon.ico",
                        "/error"
                );

        registry.addInterceptor(adminOperateLogInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        "/admin/auth/login",
                        "/favicon.ico",
                        "/error"
                );
    }
}
