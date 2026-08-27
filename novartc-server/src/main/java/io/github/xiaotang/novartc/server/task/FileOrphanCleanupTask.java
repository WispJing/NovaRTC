package io.github.xiaotang.novartc.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.server.config.FileCleanupProperties;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 孤儿文件记录定时清理任务：扫描文件信息表，将 MinIO 中确认不存在的记录逻辑删除。
 * <p>
 * 并发控制：Redisson 分布式锁保证多实例部署时同一时刻仅一个实例执行；
 * 进程内标记防止本实例上一轮未结束时重复进入（Redisson 锁可重入，
 * 同一线程重复获取仍会成功，故需进程内标记兜底）。
 * <p>
 * 安全约定：仅在 MinIO 明确应答对象不存在时才清理；网络错误、MinIO 不可用等
 * 无法确认的情况立即中止本轮，绝不在存储不可达时批量删除记录。
 */
@Component
@RequiredArgsConstructor
public class FileOrphanCleanupTask {
    private static final Logger logger = LoggerFactory.getLogger(FileOrphanCleanupTask.class);

    private final FileCleanupProperties cleanupProperties;
    private final MinioProperties minioProperties;
    private final ImFileMapper imFileMapper;
    private final MinioUtil minioUtil;
    private final RedissonClient redissonClient;

    /**
     * 进程内防重入标记
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 定时入口，按配置周期执行，时区固定 UTC。
     */
    @Scheduled(cron = "${file-cleanup.cron}", zone = "UTC")
    public void execute() {
        if (!Boolean.TRUE.equals(cleanupProperties.getEnabled())) {
            logger.debug("孤儿文件清理任务未启用，跳过执行");
            return;
        }
        cleanupOrphanFiles();
    }

    /**
     * 核心清理逻辑：获取分布式锁后分批扫描文件记录，清理 MinIO 中确认不存在的孤儿记录。
     *
     * @return 本轮清理的孤儿记录数
     */
    public int cleanupOrphanFiles() {
        if (!running.compareAndSet(false, true)) {
            logger.info("上一轮孤儿文件清理尚未结束，跳过本次执行");
            return 0;
        }
        RLock lock = redissonClient.getLock(cleanupProperties.getLockKey());
        boolean locked;
        try {
            // 零等待抢锁：未获取到说明其他实例正在执行，直接跳过
            locked = lock.tryLock(0, cleanupProperties.getLockLeaseMinutes(), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("获取孤儿文件清理分布式锁被中断，跳过本次执行");
            return 0;
        }
        if (!locked) {
            logger.info("其他实例持有孤儿文件清理锁，跳过本次执行");
            return 0;
        }
        try {
            return doCleanup();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            running.set(false);
        }
    }

    /**
     * 分批扫描并清理孤儿记录。
     */
    private int doCleanup() {
        int scanned = 0;
        int cleaned = 0;
        String bucket = minioProperties.getBucket();
        Instant cutoff = Instant.now()
                .minus(cleanupProperties.getGracePeriodHours(), ChronoUnit.HOURS);
        int batchSize = cleanupProperties.getBatchSize();
        // id 游标推进而非翻页，避免本轮逻辑删除导致记录前移被跳过
        String lastId = "";
        while (true) {
            List<ImFile> batch = fetchBatch(cutoff, lastId, batchSize);
            if (batch.isEmpty()) {
                break;
            }
            for (ImFile file : batch) {
                scanned++;
                boolean exists;
                try {
                    exists = minioUtil.objectExists(bucket, file.getObjectKey());
                } catch (ApplicationException e) {
                    logger.warn("无法确认 MinIO 对象状态，中止本轮清理: 对象={}, 原因={}",
                            file.getObjectKey(), e.getMessage());
                    return cleaned;
                }
                if (!exists) {
                    imFileMapper.deleteById(file.getId());
                    cleaned++;
                    logger.info("清理孤儿文件记录: id={}, objectKey={}", file.getId(), file.getObjectKey());
                }
            }
            lastId = batch.get(batch.size() - 1).getId();
            if (batch.size() < batchSize) {
                break;
            }
        }
        logger.info("孤儿文件清理完成: 扫描={}条, 清理孤儿记录={}条", scanned, cleaned);
        return cleaned;
    }

    /**
     * 取一批待检查记录：已过宽限期、id 大于游标的记录，按 id 升序，不查 count。
     */
    private List<ImFile> fetchBatch(Instant cutoff, String lastId, int batchSize) {
        PageHelper.startPage(1, batchSize, false);
        LambdaQueryWrapper<ImFile> wrapper = new LambdaQueryWrapper<ImFile>()
                .le(ImFile::getCreatedAt, cutoff)
                .gt(ImFile::getId, lastId)
                .orderByAsc(ImFile::getId);
        return imFileMapper.selectList(wrapper);
    }
}
