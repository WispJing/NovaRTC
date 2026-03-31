package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.common.util.MessageIdUtil;
import io.github.xiaotang.novartc.server.queue.common.InstanceIdProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 2026/3/16 16:36
 *
 * @author tangj
 * @description
 */
@Configuration
@RequiredArgsConstructor
public class MessageIdGeneratorConfig {
    private static final Logger log = LoggerFactory.getLogger(MessageIdGeneratorConfig.class);
    private static final Pattern TRAILING_NUMBER_PATTERN = Pattern.compile("(\\d+)$");
    private static final long MAX_NODE_ID = 1023L;
    private static final long WORKER_ID_MOD = 32L;

    private final IMConfigProperties imConfigProperties;
    private final InstanceIdProvider instanceIdProvider;

    @Bean
    public MessageIdUtil messageIdUtil() {
        Long configuredWorkerId = imConfigProperties.getWorkerId();
        Long configuredDatacenterId = imConfigProperties.getDatacenterId();
        if (configuredWorkerId != null && configuredDatacenterId != null) {
            log.info("Using configured snowflake node id, datacenterId={}, workerId={}", configuredDatacenterId, configuredWorkerId);
            return new MessageIdUtil(configuredWorkerId, configuredDatacenterId);
        }

        SnowflakeNodeId nodeId = resolveNodeId(instanceIdProvider.getInstanceId());
        log.info("Using derived snowflake node id from instanceId={}, datacenterId={}, workerId={}",
                instanceIdProvider.getInstanceId(), nodeId.datacenterId(), nodeId.workerId());
        return new MessageIdUtil(nodeId.workerId(), nodeId.datacenterId());
    }

    private SnowflakeNodeId resolveNodeId(String instanceId) {
        long podNodeId = resolveRawNodeId(instanceId);
        long datacenterId = podNodeId / WORKER_ID_MOD;
        long workerId = podNodeId % WORKER_ID_MOD;
        return new SnowflakeNodeId(datacenterId, workerId);
    }

    private long resolveRawNodeId(String instanceId) {
        long extractedOrdinal = extractOrdinal(instanceId);
        if (extractedOrdinal >= 0 && extractedOrdinal <= MAX_NODE_ID) {
            return extractedOrdinal;
        }

        long hashedNodeId = Math.floorMod(instanceId == null ? 0 : instanceId.hashCode(), MAX_NODE_ID + 1);
        if (extractedOrdinal > MAX_NODE_ID) {
            log.warn(
                    "Extracted ordinal {} from instanceId={} exceeds max snowflake node id {}, fallback to hashed node id={}",
                    extractedOrdinal, instanceId, MAX_NODE_ID, hashedNodeId);
        } else {
            log.warn("Could not extract pod ordinal from instanceId={}, fallback to hashed node id={}", instanceId, hashedNodeId);
        }
        return hashedNodeId;
    }

    private long extractOrdinal(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return -1L;
        }
        Matcher matcher = TRAILING_NUMBER_PATTERN.matcher(instanceId);
        if (!matcher.find()) {
            return -1L;
        }
        return Long.parseLong(matcher.group(1));
    }

    private record SnowflakeNodeId(long datacenterId, long workerId) {
    }
}
