package io.github.xiaotang.novartc.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 孤儿文件记录定时清理配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "file-cleanup")
public class FileCleanupProperties {
    /**
     * 是否启用定时清理
     */
    private Boolean enabled = true;
    /**
     * 清理周期，Spring cron 表达式（执行时区见任务的 zone 设置）
     */
    private String cron = "0 30 3 * * ?";
    /**
     * 宽限期（小时）：只清理上传时间早于该时长的记录，避免误删刚上传的文件
     */
    private Long gracePeriodHours = 24L;
    /**
     * 分批扫描每批条数
     */
    private Integer batchSize = 200;
    /**
     * 分布式锁键名，多实例部署时保证同一时刻仅一个实例执行清理
     */
    private String lockKey = "novartc:lock:file-orphan-cleanup";
    /**
     * 分布式锁持有时长上限（分钟），应大于单轮清理最长耗时，防止实例宕机后死锁
     */
    private Long lockLeaseMinutes = 30L;
}
