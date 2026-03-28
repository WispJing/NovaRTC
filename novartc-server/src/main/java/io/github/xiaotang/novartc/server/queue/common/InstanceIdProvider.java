package io.github.xiaotang.novartc.server.queue.common;

import io.github.xiaotang.novartc.server.config.IMConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Optional;
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
            instanceId = Optional.ofNullable(System.getenv("POD_NAME"))
                    .filter(name -> !name.isBlank())
                    .orElseGet(() -> {
                        try {
                            return InetAddress.getLocalHost().getHostName();
                        } catch (Exception e) {
                            return UUID.randomUUID().toString();
                        }
                    });
        } catch (Exception e) {
            instanceId = UUID.randomUUID().toString();
        }
    }
}
