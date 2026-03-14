package io.github.xiaotang.novartc.common.util;

import lombok.Getter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 流式批处理执行器，支持多线程并发处理、可配置重试策略和失败模式。
 *
 * @author tangj
 * @since 2026/3/14
 */
public class StreamBatchExecutor implements AutoCloseable {

    private final ExecutorService executor;
    private final Semaphore semaphore;
    private final RetryPolicy defaultRetryPolicy;

    /**
     * 构造批处理执行器。
     *
     * @param threads        线程池核心线程数（也作为最大线程数）
     * @param maxConcurrency 最大并发任务数（通过信号量控制）
     */
    public StreamBatchExecutor(int threads, int maxConcurrency) {
        this(threads, maxConcurrency, RetryPolicy.none());
    }

    /**
     * 构造批处理执行器，并指定默认重试策略。
     *
     * @param threads            线程池核心线程数
     * @param maxConcurrency     最大并发任务数
     * @param defaultRetryPolicy 默认重试策略（当未显式传入时使用）
     */
    public StreamBatchExecutor(int threads, int maxConcurrency, RetryPolicy defaultRetryPolicy) {
        this.executor = new ThreadPoolExecutor(
                threads,
                threads,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        this.semaphore = new Semaphore(maxConcurrency);
        this.defaultRetryPolicy = defaultRetryPolicy;
    }

    /**
     * 执行批量处理（使用默认重试策略，快速失败模式）。
     *
     * @param pageSize    每页大小（用于从数据源获取数据）
     * @param pageFetcher 分页获取数据的函数，参数为页码（从1开始），返回该页数据
     * @param processor   处理一批数据的函数，输入一批数据，输出处理结果列表
     * @param <T>         原始数据类型
     * @param <R>         结果类型
     * @return 所有处理结果的合并列表
     */
    public <T, R> List<R> execute(
            int pageSize,
            Function<Integer, List<T>> pageFetcher,
            Function<List<T>, List<R>> processor) {
        return execute(pageSize, pageFetcher, processor, defaultRetryPolicy, FailureMode.FAIL_FAST);
    }

    /**
     * 执行批量处理（可指定重试策略和失败模式）。
     *
     * @param pageSize    每页大小
     * @param pageFetcher 分页获取数据的函数
     * @param processor   处理一批数据的函数
     * @param retryPolicy 重试策略（为空表示不重试）
     * @param failureMode 失败模式：FAIL_FAST 遇到失败立即抛出异常；CONTINUE 继续后续处理，但记录失败信息
     * @param <T>         原始数据类型
     * @param <R>         结果类型
     * @return 处理结果列表（当失败模式为 CONTINUE 时，仅包含成功批次的结果）
     * @throws RuntimeException 如果某个批次处理失败且失败模式为 FAIL_FAST，则抛出异常
     */
    public <T, R> List<R> execute(
            int pageSize,
            Function<Integer, List<T>> pageFetcher,
            Function<List<T>, List<R>> processor,
            RetryPolicy retryPolicy,
            FailureMode failureMode) {

        List<R> results = Collections.synchronizedList(new ArrayList<>());
        List<BatchFailure> failures = Collections.synchronizedList(new ArrayList<>());

        int page = 1;
        while (true) {
            List<T> pageData = pageFetcher.apply(page);
            if (pageData == null || pageData.isEmpty()) {
                break;
            }

            // 将一页数据拆分为多个批次
            List<List<T>> batches = split(pageData, pageSize);

            // 提交所有批次任务
            int finalPage = page;
            List<CompletableFuture<Void>> futures = batches.stream()
                    .map(batch -> CompletableFuture.runAsync(() -> {
                        try {
                            semaphore.acquire();
                            // 执行带重试的处理
                            List<R> batchResult = executeWithRetry(batch, processor, retryPolicy);
                            results.addAll(batchResult);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        } catch (Throwable t) {
                            // 处理失败
                            if (failureMode == FailureMode.FAIL_FAST) {
                                throw new CompletionException(t);
                            } else {
                                // 记录失败信息，但不中断流程
                                failures.add(new BatchFailure(finalPage, batch, t));
                            }
                        } finally {
                            semaphore.release();
                        }
                    }, executor))
                    .toList();

            // 等待当前页所有批次完成
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException e) {
                // 如果是快速失败模式，直接抛出原始异常
                throw new RuntimeException("Batch processing failed at page " + page, e.getCause());
            }

            System.out.println("batch page finished: " + page);
            page++;
        }

        // 如果失败模式为 CONTINUE 且有失败记录，可以打印日志或通过其他方式通知调用者
        if (!failures.isEmpty()) {
            System.err.println("Some batches failed: " + failures.size() + " failures.");
            // 可以将 failures 存入某个上下文或通过回调处理
        }

        return results;
    }

    /**
     * 直接处理整个 List，内部自动分页。
     *
     * @param list      待处理的完整数据列表
     * @param pageSize  每页大小（决定每次从 list 中切出多少元素）
     * @param batchSize 每个批次的大小（在页内再切分）
     * @param processor 处理函数
     * @param <T>       原始数据类型
     * @param <R>       结果类型
     * @return 处理结果列表
     */
    public <T, R> List<R> executeList(
            List<T> list,
            int pageSize,
            int batchSize,
            Function<List<T>, List<R>> processor) {
        return executeList(list, pageSize, batchSize, processor, defaultRetryPolicy, FailureMode.FAIL_FAST);
    }

    /**
     * 直接处理整个 List，可指定重试策略和失败模式。
     *
     * @param list        待处理的完整数据列表
     * @param pageSize    每页大小
     * @param batchSize   每个批次的大小
     * @param processor   处理函数
     * @param retryPolicy 重试策略
     * @param failureMode 失败模式
     * @param <T>         原始数据类型
     * @param <R>         结果类型
     * @return 处理结果列表
     */
    public <T, R> List<R> executeList(
            List<T> list,
            int pageSize,
            int batchSize,
            Function<List<T>, List<R>> processor,
            RetryPolicy retryPolicy,
            FailureMode failureMode) {

        return execute(
                batchSize, // 注意：这里 pageSize 参数实际上控制的是每批次的大小？需要区分
                page -> {
                    int start = (page - 1) * pageSize;
                    if (start >= list.size()) {
                        return Collections.emptyList();
                    }
                    int end = Math.min(start + pageSize, list.size());
                    return list.subList(start, end);
                },
                processor,
                retryPolicy,
                failureMode
        );
    }

    /**
     * 带重试的执行处理。
     */
    private <T, R> List<R> executeWithRetry(
            List<T> batch,
            Function<List<T>, List<R>> processor,
            RetryPolicy retryPolicy) {

        int attempts = 0;
        Throwable lastError = null;
        while (attempts < retryPolicy.getMaxAttempts()) {
            try {
                return processor.apply(batch);
            } catch (Throwable t) {
                lastError = t;
                attempts++;
                if (attempts >= retryPolicy.getMaxAttempts()) {
                    break; // 达到最大尝试次数，退出循环
                }
                // 判断是否需要重试
                if (!retryPolicy.getRetryCondition().test(t)) {
                    break; // 条件不满足，不再重试
                }
                // 执行退避延迟
                try {
                    TimeUnit.MILLISECONDS.sleep(retryPolicy.getDelayMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        // 所有重试均失败，抛出异常
        throw new RuntimeException("Batch processing failed after " + attempts + " attempts", lastError);
    }

    /**
     * 拆分列表为多个子批次。
     */
    private <T> List<List<T>> split(List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            result.add(list.subList(i, end));
        }
        return result;
    }

    /**
     * 关闭线程池。
     */
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public void close() {
        shutdown();
    }

    /**
     * 重试策略配置类。
     */
    @Getter
    public static class RetryPolicy {
        private final int maxAttempts;
        private final long delayMillis;
        private final Predicate<Throwable> retryCondition;

        private RetryPolicy(int maxAttempts, long delayMillis, Predicate<Throwable> retryCondition) {
            this.maxAttempts = maxAttempts;
            this.delayMillis = delayMillis;
            this.retryCondition = retryCondition;
        }

        /**
         * 创建不重试的策略。
         */
        public static RetryPolicy none() {
            return new RetryPolicy(1, 0, t -> false);
        }

        /**
         * 创建默认重试策略：最多重试3次，每次延迟100ms，对所有异常都重试。
         */
        public static RetryPolicy defaultPolicy() {
            return new RetryPolicy(3, 100, t -> true);
        }

        /**
         * 创建自定义重试策略。
         *
         * @param maxAttempts    最大尝试次数（包含第一次）
         * @param delayMillis    重试间隔毫秒
         * @param retryCondition 判定是否应该重试的条件
         * @return RetryPolicy 实例
         */
        public static RetryPolicy custom(int maxAttempts, long delayMillis, Predicate<Throwable> retryCondition) {
            return new RetryPolicy(maxAttempts, delayMillis, retryCondition);
        }

    }

    /**
     * 失败模式枚举。
     */
    public enum FailureMode {
        /**
         * 快速失败：任何批次失败立即抛出异常，中断处理。
         */
        FAIL_FAST,
        /**
         * 继续处理：记录失败信息，继续处理后续批次。
         */
        CONTINUE
    }

    /**
     * 记录批次失败信息的内部类。
     */
    private static class BatchFailure {
        final int page;
        final List<?> batchData;
        final Throwable error;

        BatchFailure(int page, List<?> batchData, Throwable error) {
            this.page = page;
            this.batchData = batchData;
            this.error = error;
        }
    }

    public static void main(String[] args) {
        // 创建执行器（10个线程，最大并发5）
        try (StreamBatchExecutor executor = new StreamBatchExecutor(10, 5)) {
            // 自定义重试策略：最多重试2次，延迟200ms，仅对 IOException 重试
            RetryPolicy retryPolicy = RetryPolicy.custom(2, 200,
                    t -> t instanceof IOException);

            /*List<Result> results = executor.execute(
                    100,                            // 每页大小
                    page -> fetchData(page),         // 分页获取数据
                    batch -> processBatch(batch),    // 处理批次
                    retryPolicy,
                    FailureMode.CONTINUE              // 遇到失败继续处理
            );*/
        } catch (Exception e) {
            // 处理异常
        }
    }
}
