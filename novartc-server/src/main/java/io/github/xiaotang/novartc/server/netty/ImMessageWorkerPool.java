package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatMsgLog;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * IM 消息业务线程池。
 * <p>
 * 按路由键（发送者 userId）哈希分桶到 N 个单线程执行器：
 * 同一路由键的消息严格串行处理，保证持久化序号与下发顺序不乱；
 * 不同路由键的消息并行处理，避免阻塞 Netty IO 线程。
 */
@Component
public class ImMessageWorkerPool {

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final ExecutorService[] workers;

    public ImMessageWorkerPool() {
        int workerCount = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        this.workers = new ExecutorService[workerCount];
        for (int i = 0; i < workerCount; i++) {
            final int index = i;
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, "IM-BIZ-" + index);
                thread.setUncaughtExceptionHandler((t, e) ->
                        ChatMsgLog.logger.error("IM 业务线程未捕获异常: thread={}", t.getName(), e));
                return thread;
            };
            this.workers[i] = Executors.newSingleThreadExecutor(threadFactory);
        }
        ChatMsgLog.logger.info("IM 消息业务线程池初始化完成: workerCount={}", workerCount);
    }

    /**
     * 按路由键哈希提交任务，相同路由键的任务严格串行执行。
     *
     * @param routingKey 路由键（通常为发送者 userId）
     * @param task       业务任务
     */
    public void submit(String routingKey, Runnable task) {
        workers[bucketOf(routingKey)].execute(task);
    }

    /**
     * 计算路由键对应的分桶序号。
     */
    int bucketOf(String routingKey) {
        return Math.floorMod(routingKey == null ? 0 : routingKey.hashCode(), workers.length);
    }

    @PreDestroy
    public void shutdown() {
        for (ExecutorService worker : workers) {
            worker.shutdown();
        }
        for (ExecutorService worker : workers) {
            try {
                if (!worker.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    worker.shutdownNow();
                }
            } catch (InterruptedException e) {
                worker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        ChatMsgLog.logger.info("IM 消息业务线程池已关闭");
    }
}
