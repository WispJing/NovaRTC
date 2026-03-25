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
    private Long workerId;
    private Long datacenterId;
}
