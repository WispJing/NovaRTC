package io.github.xiaotang.novartc.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度平台对接配置：执行通知消费 + 执行结果回传。
 *
 * @author xiaotang
 */
@Data
@Component
@ConfigurationProperties(prefix = "job")
public class JobMqProperties {

    /**
     * 任务执行通知 topic（调度平台 -> NovaRTC 服务）
     */
    private String topic = "novartc-job-topic";

    /**
     * 执行结果回传 topic（NovaRTC 服务 -> 调度平台）
     */
    private String resultTopic = "novartc-job-result-topic";

    /**
     * 执行通知消费者组（集群模式，组内仅一个实例执行任务）
     */
    private String consumerGroup = "novartc-job-consumer-group";

    /**
     * 允许反射执行的类包名前缀白名单
     */
    private String allowedPackagePrefix = "io.github.xiaotang.novartc";
}
