package io.github.xiaotang.novartc.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created on 2026/3/10 14:11
 *
 * @author tangj
 * @description
 */
@Data
@Component
@ConfigurationProperties(prefix = "im")
public class IMConfigProperties {
    private String host;
    private Integer port;
    private String userInstancesKeyPreFix;
    private String instanceKeyPreFix;
    private String chatSeqKeyPreFix;
    private String chatTopic;
    private String chatConsumerGroup;
    private String presenceTopic;
    private String presenceConsumerGroup;
    private String tokenKickTopic;
    private String tokenKickConsumerGroup;
    private String userFriendCacheKeyPreFix;
    private String userGroupCacheKeyPreFix;
    private String groupMembersCacheKeyPreFix;
    private Long workerId;
    private Long datacenterId;
    /**
     * IM 业务处理分桶数，0 表示按 CPU 核数自动计算。
     */
    private Integer messageWorkerCount = 0;
    /**
     * 每个 IM 业务分桶允许排队的最大消息数。
     */
    private Integer messageWorkerQueueCapacity = 1024;
    /**
     * IM 业务线程池优雅关闭等待时间（秒）。
     */
    private Integer messageWorkerShutdownTimeoutSeconds = 10;
    /**
     * 单实例最大 IM 连接数，0 表示不限制
     */
    private Long maxConnections;
}
