package io.github.xiaotang.novartc.server.netty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IM 消息工作线程池测试，验证按路由键分桶的并发模型：
 * 同键任务严格串行有序，不同键任务并行互不阻塞。
 */
class ImMessageWorkerPoolTest {

    private ImMessageWorkerPool pool;

    /**
     * 每个用例结束后关闭线程池，避免线程泄漏影响其他测试。
     */
    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    /**
     * 同一路由键的 1000 个任务必须严格按提交顺序执行，
     * 保证同一用户的消息处理顺序不乱。
     */
    @Test
    void sameKeyTasksExecuteInOrder() throws InterruptedException {
        pool = new ImMessageWorkerPool(4, 1024, 10);
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

    /**
     * 不同路由键（落在不同分桶）的任务互不阻塞：
     * 慢任务执行期间，快任务应及时完成，且两者运行在不同线程上。
     */
    @Test
    void differentKeysDoNotBlockEachOther() throws InterruptedException {
        pool = new ImMessageWorkerPool(4, 1024, 10);
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

    /**
     * 分桶队列达到上限时必须快速拒绝，不能在 Netty IO 线程上阻塞或无限积压。
     */
    @Test
    void fullBucketRejectsNewTask() throws InterruptedException {
        pool = new ImMessageWorkerPool(1, 1, 1);
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        assertTrue(pool.submit("user-1", () -> {
            running.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertTrue(running.await(5, TimeUnit.SECONDS));
        assertTrue(pool.submit("user-1", () -> { }));

        assertFalse(pool.submit("user-1", () -> { }));
        release.countDown();
    }

    /**
     * 辅助方法：找到一个与给定键落在不同分桶的路由键。
     */
    private String findKeyInDifferentBucket(String baseKey) {
        for (int i = 0; ; i++) {
            String candidate = "candidate-" + i;
            if (pool.bucketOf(candidate) != pool.bucketOf(baseKey)) {
                return candidate;
            }
        }
    }
}
