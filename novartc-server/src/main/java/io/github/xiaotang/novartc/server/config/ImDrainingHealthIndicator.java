package io.github.xiaotang.novartc.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 排空状态健康指示器。
 * <p>
 * 并入 readiness 组后，实例进入排空状态时探针失败，
 * K8s 将其移出 Service endpoints，停止导入新流量。
 */
@Component
@RequiredArgsConstructor
public class ImDrainingHealthIndicator implements HealthIndicator {

    private final InstanceDrainState drainState;

    @Override
    public Health health() {
        if (drainState.isDraining()) {
            return Health.down().withDetail("reason", "instance draining").build();
        }
        return Health.up().build();
    }
}
