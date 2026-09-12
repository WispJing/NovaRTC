package io.github.xiaotang.novartc.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务 RocketMQ 消息配置。
 *
 * @author xiaotang
 */
@Data
@Component
@ConfigurationProperties(prefix = "job")
public class JobMqProperties {
    /** 可被调度的服务白名单，新增服务必须显式加入。 */
    private List<String> allowedServices = List.of("novartc-server");

    /**
     * 任务执行通知 topic（调度平台 -> NovaRTC 服务）
     */
    private String topic = "novartc-job-topic";

    /**
     * 执行结果回传 topic（NovaRTC 服务 -> 调度平台）
     */
    private String resultTopic = "novartc-job-result-topic";

    /**
     * 结果回传消费者组
     */
    private String resultConsumerGroup = "novartc-job-result-consumer-group";

    /**
     * 任务从触发到等待结果的最大时长，超时后将 PENDING/PROCESSING 日志收敛为失败。
     * 该值应大于最长处理器执行时间及消息传输延迟，避免仍在运行的任务被提前判定失败。
     */
    private long resultTimeoutSeconds = 3600;
}
