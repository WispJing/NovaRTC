package io.github.xiaotang.novartc.server.task;

import io.github.xiaotang.novartc.common.job.JobHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link JobHandler} 注解元信息校验：确保任务声明携带名称与备注，
 * 为后续扫描器写入 job_info 提供稳定输入。
 *
 * @author xiaotang
 */
class JobHandlerAnnotationTest {

    /**
     * 失效文件记录清理入口标注 @JobHandler 且名称、备注完整
     */
    @Test
    void executeMethodDeclaresJobHandler() throws NoSuchMethodException {
        Method method = InvalidFileRecordCleanupTask.class.getMethod("execute");
        JobHandler handler = method.getAnnotation(JobHandler.class);
        assertNotNull(handler, "execute 方法缺少 @JobHandler 注解");
        assertEquals("失效文件记录清理", handler.jobName());
        assertEquals("扫描文件信息表，逻辑删除 MinIO 中确认不存在的记录", handler.remark());
    }
}
