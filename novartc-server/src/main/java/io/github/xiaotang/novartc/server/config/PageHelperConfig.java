package io.github.xiaotang.novartc.server.config;

import com.github.pagehelper.PageInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * PageHelper 分页配置。
 * <p>
 * 以 Interceptor Bean 形式注册，由 MyBatis-Plus 自动配置收集并注入
 * SqlSessionFactory；不使用 pagehelper-spring-boot-starter，
 * 避免其引入的 mybatis 自动配置与 MyBatis-Plus 冲突。
 */
@Configuration
public class PageHelperConfig {

    /**
     * 分页拦截器，显式指定 MySQL 方言。
     */
    @Bean
    public PageInterceptor pageInterceptor() {
        PageInterceptor interceptor = new PageInterceptor();
        Properties properties = new Properties();
        properties.setProperty("helperDialect", "mysql");
        interceptor.setProperties(properties);
        return interceptor;
    }
}
