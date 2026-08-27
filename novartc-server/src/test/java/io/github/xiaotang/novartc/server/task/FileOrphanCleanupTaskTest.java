package io.github.xiaotang.novartc.server.task;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.server.config.FileCleanupProperties;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.util.MinioUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 孤儿文件记录清理任务单元测试，不启动 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
class FileOrphanCleanupTaskTest {

    @Mock
    private ImFileMapper imFileMapper;

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    private FileCleanupProperties cleanupProperties;

    private MinioProperties minioProperties;

    private FileOrphanCleanupTask cleanupTask;

    @BeforeEach
    void setUp() {
        cleanupProperties = new FileCleanupProperties();
        cleanupProperties.setGracePeriodHours(24L);
        cleanupProperties.setBatchSize(200);
        minioProperties = new MinioProperties();
        minioProperties.setBucket("novartc");
        cleanupTask = new FileOrphanCleanupTask(
                cleanupProperties, minioProperties, imFileMapper, minioUtil, redissonClient);
    }

    private void mockLockAcquired() throws InterruptedException {
        when(redissonClient.getLock(cleanupProperties.getLockKey())).thenReturn(lock);
        when(lock.tryLock(0, cleanupProperties.getLockLeaseMinutes(), TimeUnit.MINUTES))
                .thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    private ImFile buildFile(String id, String objectKey) {
        ImFile file = new ImFile();
        file.setId(id);
        file.setObjectKey(objectKey);
        file.setCreatedAt(Instant.now().minusSeconds(3600 * 48));
        return file;
    }

    @Test
    void cleanupShouldDeleteOnlyOrphanRecords() throws InterruptedException {
        mockLockAcquired();
        ImFile existing = buildFile("aaa", "avatar/20260826/a.png");
        ImFile orphan = buildFile("bbb", "file/20260826/b.png");
        when(imFileMapper.selectList(any()))
                .thenReturn(List.of(existing, orphan))
                .thenReturn(Collections.emptyList());
        when(minioUtil.objectExists("novartc", "avatar/20260826/a.png")).thenReturn(true);
        when(minioUtil.objectExists("novartc", "file/20260826/b.png")).thenReturn(false);

        int cleaned = cleanupTask.cleanupOrphanFiles();

        assertThat(cleaned).isEqualTo(1);
        verify(imFileMapper, never()).deleteById("aaa");
        verify(imFileMapper, times(1)).deleteById("bbb");
        verify(lock, times(1)).unlock();
    }

    @Test
    void cleanupShouldKeepAllRecordsWhenObjectsExist() throws InterruptedException {
        mockLockAcquired();
        ImFile existing = buildFile("aaa", "avatar/20260826/a.png");
        when(imFileMapper.selectList(any()))
                .thenReturn(List.of(existing))
                .thenReturn(Collections.emptyList());
        when(minioUtil.objectExists("novartc", "avatar/20260826/a.png")).thenReturn(true);

        int cleaned = cleanupTask.cleanupOrphanFiles();

        assertThat(cleaned).isZero();
        verify(imFileMapper, never()).deleteById(anyString());
    }

    @Test
    void cleanupShouldAbortWhenMinioStateUncertain() throws InterruptedException {
        mockLockAcquired();
        ImFile uncertain = buildFile("aaa", "avatar/20260826/a.png");
        ImFile orphan = buildFile("bbb", "file/20260826/b.png");
        when(imFileMapper.selectList(any())).thenReturn(List.of(uncertain, orphan));
        when(minioUtil.objectExists("novartc", "avatar/20260826/a.png"))
                .thenThrow(new ApplicationException("判断对象存在失败: novartc/avatar/20260826/a.png",
                        new RuntimeException("连接超时"), ResultCode.SERVER_ERROR.code()));

        int cleaned = cleanupTask.cleanupOrphanFiles();

        // 无法确认状态时中止本轮，任何记录都不删除，锁仍要释放
        assertThat(cleaned).isZero();
        verify(imFileMapper, never()).deleteById(anyString());
        verify(lock, times(1)).unlock();
    }

    @Test
    void cleanupShouldSkipWhenLockHeldByOtherInstance() throws InterruptedException {
        when(redissonClient.getLock(cleanupProperties.getLockKey())).thenReturn(lock);
        when(lock.tryLock(0, cleanupProperties.getLockLeaseMinutes(), TimeUnit.MINUTES))
                .thenReturn(false);

        int cleaned = cleanupTask.cleanupOrphanFiles();

        assertThat(cleaned).isZero();
        verify(imFileMapper, never()).selectList(any());
        verify(imFileMapper, never()).deleteById(anyString());
    }
}
