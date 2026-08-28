-- 定时任务更名：“孤儿文件记录清理” -> “失效文件记录清理”。
-- 执行类 FileOrphanCleanupTask 已重命名为 InvalidFileRecordCleanupTask，
-- 存量任务定义的 invoke_class 一并更新，否则调度触发时将找不到执行 Bean。
UPDATE `job_info`
SET `job_name`     = '失效文件记录清理',
    `invoke_class` = 'io.github.xiaotang.novartc.server.task.InvalidFileRecordCleanupTask'
WHERE `invoke_class` = 'io.github.xiaotang.novartc.server.task.FileOrphanCleanupTask';
