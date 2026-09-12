package io.github.xiaotang.novartc.server.netty;

import io.github.xiaotang.novartc.server.config.ChatMsgLog;
import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
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

    private static final int MIN_WORKER_COUNT = 1;
    private static final int DEFAULT_QUEUE_CAPACITY = 1024;
    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final ThreadPoolExecutor[] workers;
    private final int shutdownTimeoutSeconds;

    @Autowired
    public ImMessageWorkerPool(IMConfigProperties properties) {
        this(resolveWorkerCount(properties),
                positiveOrDefault(properties.getMessageWorkerQueueCapacity(), DEFAULT_QUEUE_CAPACITY),
                positiveOrDefault(properties.getMessageWorkerShutdownTimeoutSeconds(),
                        DEFAULT_SHUTDOWN_TIMEOUT_SECONDS));
    }

    ImMessageWorkerPool(int workerCount, int queueCapacity, int shutdownTimeoutSeconds) {
        if (workerCount < MIN_WORKER_COUNT) {
            throw new IllegalArgumentException("IM 业务线程数必须大于 0");
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("IM 业务队列容量必须大于 0");
        }
        if (shutdownTimeoutSeconds < 1) {
            throw new IllegalArgumentException("IM 业务线程池关闭等待时间必须大于 0");
        }
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        this.workers = new ThreadPoolExecutor[workerCount];
        for (int i = 0; i < workerCount; i++) {
            final int index = i;
            ThreadFactory threadFactory = runnable -> {
                Thread thread = new Thread(runnable, "IM-BIZ-" + index);
                thread.setUncaughtExceptionHandler((t, e) ->
                        ChatMsgLog.logger.error("IM 业务线程未捕获异常: thread={}", t.getName(), e));
                return thread;
            };
            this.workers[i] = new ThreadPoolExecutor(
                    1,
                    1,
                    0,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueCapacity),
                    threadFactory,
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }
        ChatMsgLog.logger.info("IM 消息业务线程池初始化完成: workerCount={}, queueCapacity={}",
                workerCount, queueCapacity);
    }

    /**
     * 按路由键哈希提交任务，相同路由键的任务严格串行执行。
     *
     * @param routingKey 路由键（通常为发送者 userId）
     * @param task       业务任务
     */
    public boolean submit(String routingKey, Runnable task) {
        int bucket = bucketOf(routingKey);
        try {
            workers[bucket].execute(task);
            return true;
        } catch (RejectedExecutionException e) {
            ChatMsgLog.logger.warn(
                    "IM 业务队列已满或线程池已关闭，拒绝消息: routingKey={}, bucket={}, queueSize={}",
                    routingKey, bucket, workers[bucket].getQueue().size());
            return false;
        }
    }

    /**
     * 计算路由键对应的分桶序号。
     */
    int bucketOf(String routingKey) {
        return Math.floorMod(routingKey == null ? 0 : routingKey.hashCode(), workers.length);
    }

    @PreDestroy
    public void shutdown() {
        for (ThreadPoolExecutor worker : workers) {
            worker.shutdown();
        }
        for (ThreadPoolExecutor worker : workers) {
            try {
                if (!worker.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                    worker.shutdownNow();
                }
            } catch (InterruptedException e) {
                worker.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        ChatMsgLog.logger.info("IM 消息业务线程池已关闭");
    }

    private static int resolveWorkerCount(IMConfigProperties properties) {
        Integer configured = properties.getMessageWorkerCount();
        if (configured == null || configured == 0) {
            return Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        }
        if (configured > 0) {
            return configured;
        }
        throw new IllegalArgumentException("IM 业务线程数不能小于 0");
    }

    private static int positiveOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}
