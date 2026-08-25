package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.server.netty.LocalSessionDispatcher;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;

/**
 * IM 连接数监控指标注册。
 */
@Configuration
@RequiredArgsConstructor
public class ConnectionMetricsConfig {

    private final LocalSessionDispatcher localSessionDispatcher;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @PostConstruct
    public void registerGauge() {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        Gauge.builder("im.connections.active", localSessionDispatcher, LocalSessionDispatcher::getConnectionCount)
                .description("当前已认证 IM 连接数")
                .register(registry);
    }
}
