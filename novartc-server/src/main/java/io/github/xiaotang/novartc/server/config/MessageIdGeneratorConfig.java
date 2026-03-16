package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.common.util.MessageIdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created on 2026/3/16 16:36
 *
 * @author tangj
 * @description
 */
@Configuration
public class MessageIdGeneratorConfig {
    @Bean
    public MessageIdUtil messageIdUtil() {
        // 从配置文件读取 workerId 和 datacenterId
        long workerId = 1L;
        long datacenterId = 1L;
        return new MessageIdUtil(workerId, datacenterId);
    }
}
