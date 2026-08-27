package io.github.xiaotang.novartc.server.task;

import io.github.xiaotang.novartc.common.job.JobHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link JobHandler} 注解元信息校验：确保任务声明携带完整调度信息，
 * 为后续扫描器写入 job_info 提供稳定输入。
 *
 * @author xiaotang
 */
class JobHandlerAnnotationTest {

    /**
     * 孤儿文件清理入口标注 @JobHandler 且调度信息完整
     */
    @Test
    void executeMethodDeclaresJobHandler() throws NoSuchMethodException {
        Method method = FileOrphanCleanupTask.class.getMethod("execute");
        JobHandler handler = method.getAnnotation(JobHandler.class);
        assertNotNull(handler, "execute 方法缺少 @JobHandler 注解");
        assertEquals("孤儿文件记录清理", handler.jobName());
        assertEquals("${file-cleanup.cron}", handler.cron());
        assertEquals(0, handler.fixedRateSeconds());
        assertFalse(handler.concurrentAllowed());
    }
}
