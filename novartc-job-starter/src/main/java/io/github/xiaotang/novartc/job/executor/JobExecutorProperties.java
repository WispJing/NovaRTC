package io.github.xiaotang.novartc.job.executor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("novartc.job.executor")
public class JobExecutorProperties {
    /** 本服务稳定且唯一的 kebab-case 标识，也是 RocketMQ Tag。 */
    private String serviceName;
    /** 调度中心发送任务的统一 Topic。 */
    private String topic = "novartc-job-topic";
    /** 执行端回传执行结果的统一 Topic。 */
    private String resultTopic = "novartc-job-result-topic";
    /** 空值时使用基于服务名的默认消费组，隔离不同服务。 */
    private String consumerGroup;
    /**
     * RocketMQ 消费线程池的最小线程数。长任务默认只并行执行两个，避免占满服务实例。
     */
    private int consumeThreadMin = 2;
    /** RocketMQ 消费线程池的最大线程数；默认与最小值相同，固定长任务并发度。 */
    private int consumeThreadMax = 2;
    /**
     * RocketMQ 单条消息的消费超时，单位分钟。该值应大于最长任务执行时间，
     * 否则任务未完成时可能被判定为超时并重复投递。
     */
    private long consumeTimeoutMinutes = 35;
    /** 任务执行线程显示前缀；不影响服务标识、消费组或消息 Tag。 */
    private String threadNamePrefix = "job-exec-";
    /**
     * 执行实例标识；空值时自动使用“服务名@主机名”。多副本部署可显式配置 Pod 名称。
     */
    private String instanceId;
}
