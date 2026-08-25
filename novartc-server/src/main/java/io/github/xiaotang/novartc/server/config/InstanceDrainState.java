package io.github.xiaotang.novartc.server.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 实例排空状态，供优雅关停服务与 readiness 健康指示器共享。
 */
@Component
public class InstanceDrainState {

    private final AtomicBoolean draining = new AtomicBoolean(false);

    public void markDraining() {
        draining.set(true);
    }

    public boolean isDraining() {
        return draining.get();
    }
}
