package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.common.util.MessageIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created on 2026/3/16 16:36
 *
 * @author tangj
 * @description
 */
@Configuration
@RequiredArgsConstructor
public class MessageIdGeneratorConfig {
    private final IMConfigProperties imConfigProperties;

    @Bean
    public MessageIdUtil messageIdUtil() {
        long workerId = imConfigProperties.getWorkerId() == null ? 1L : imConfigProperties.getWorkerId();
        long datacenterId = imConfigProperties.getDatacenterId() == null ? 1L : imConfigProperties.getDatacenterId();
        return new MessageIdUtil(workerId, datacenterId);
    }
}
