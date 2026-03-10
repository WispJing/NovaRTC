package io.github.xiaotang.novartc.server.queue.common;

import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.UUID;

/**
 * Created on 2026/3/10 11:36
 *
 * @author tangj
 * @description
 */
@Getter
@Component
public class InstanceIdProvider {
    @Autowired
    private IMConfigProperties imConfigProperties;

    private String instanceId;

    @PostConstruct
    public void init() {
        try {
            instanceId = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            instanceId = UUID.randomUUID().toString();
        }
    }
}
