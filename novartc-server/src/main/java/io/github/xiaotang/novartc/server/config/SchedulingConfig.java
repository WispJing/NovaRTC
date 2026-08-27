package io.github.xiaotang.novartc.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置，启用 {@code @Scheduled} 支持。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
