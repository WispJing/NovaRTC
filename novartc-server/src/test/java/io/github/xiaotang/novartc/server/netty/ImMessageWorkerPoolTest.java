package io.github.xiaotang.novartc.server.netty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImMessageWorkerPoolTest {

    private ImMessageWorkerPool pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Test
    void sameKeyTasksExecuteInOrder() throws InterruptedException {
        pool = new ImMessageWorkerPool();
        int taskCount = 1000;
        List<Integer> results = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            final int value = i;
            pool.submit("user-1", () -> {
                results.add(value);
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "任务未在预期时间内完成");
        assertEquals(taskCount, results.size());
        for (int i = 0; i < taskCount; i++) {
            assertEquals(i, results.get(i), "同一路由键的任务必须严格串行有序");
        }
    }

    @Test
    void differentKeysDoNotBlockEachOther() throws InterruptedException {
        pool = new ImMessageWorkerPool();
        String slowKey = "user-slow";
        String fastKey = findKeyInDifferentBucket(slowKey);

        CountDownLatch slowStarted = new CountDownLatch(1);
        CountDownLatch slowDone = new CountDownLatch(1);
        CountDownLatch fastDone = new CountDownLatch(1);
        AtomicReference<String> slowThread = new AtomicReference<>();
        AtomicReference<String> fastThread = new AtomicReference<>();

        pool.submit(slowKey, () -> {
            slowThread.set(Thread.currentThread().getName());
            slowStarted.countDown();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            slowDone.countDown();
        });
        assertTrue(slowStarted.await(5, TimeUnit.SECONDS), "慢任务未启动");

        long fastStart = System.nanoTime();
        pool.submit(fastKey, () -> {
            fastThread.set(Thread.currentThread().getName());
            fastDone.countDown();
        });
        assertTrue(fastDone.await(5, TimeUnit.SECONDS), "快任务未在预期时间内完成");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - fastStart);

        assertTrue(elapsedMs < 300, "不同路由键的任务不应互相阻塞，实际等待 " + elapsedMs + "ms");
        assertNotEquals(slowThread.get(), fastThread.get(), "不同分桶应使用不同线程");
        assertTrue(slowDone.await(5, TimeUnit.SECONDS));
    }

    private String findKeyInDifferentBucket(String baseKey) {
        for (int i = 0; ; i++) {
            String candidate = "candidate-" + i;
            if (pool.bucketOf(candidate) != pool.bucketOf(baseKey)) {
                return candidate;
            }
        }
    }
}
