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
    /** 任务执行线程显示前缀；不影响服务标识、消费组或消息 Tag。 */
    private String threadNamePrefix = "job-exec-";
}
