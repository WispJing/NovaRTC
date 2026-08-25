package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatAppLog;
import io.github.xiaotang.novartc.server.config.InstanceDrainState;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import io.github.xiaotang.novartc.server.queue.common.UserRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * IM 服务优雅关停。
 * <p>
 * 实例退出前依次执行：标记排空（readiness 转 DOWN 使 K8s 摘流）、
 * 停止接受新连接、摘除用户路由、通知并断开存量连接，
 * 避免长连接被粗暴切断导致用户掉线无感知。
 */
@Component
@RequiredArgsConstructor
public class ImGracefulShutdownService implements SmartLifecycle {

    private static final long FLUSH_WINDOW_MILL = 3000L;

    private final NovaRTCServer novaRtcServer;
    private final LocalSessionDispatcher localSessionDispatcher;
    private final UserRouteService userRouteService;
    private final InstanceIdProvider instanceIdProvider;
    private final InstanceDrainState drainState;

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        drain();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // 最大 phase 最先停止，保证排空早于其余组件销毁
        return Integer.MAX_VALUE;
    }

    private void drain() {
        String instanceId = instanceIdProvider.getInstanceId();
        drainState.markDraining();
        ChatAppLog.log.info("实例进入排空状态: instanceId={}", instanceId);

        // 1. 停止接受新连接
        novaRtcServer.stopAccepting();

        // 2. 立即摘除路由表，避免新消息继续路由到本实例
        for (String userId : localSessionDispatcher.getConnectedUserIds()) {
            userRouteService.unregisterUser(userId, instanceId);
        }

        // 3. 通知存量连接并关闭，引导客户端重连到其他实例
        localSessionDispatcher.closeAllWithNotice("服务器正在维护，即将恢复，请稍候");

        // 4. 刷盘窗口，确保通知写入通道
        try {
            Thread.sleep(FLUSH_WINDOW_MILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        novaRtcServer.shutdown();
        ChatAppLog.log.info("NovaRTC IM 服务已停止: instanceId={}", instanceId);
    }
}
