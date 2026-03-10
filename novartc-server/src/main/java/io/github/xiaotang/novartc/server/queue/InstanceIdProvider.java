package io.github.xiaotang.novartc.server.queue;

import io.github.xiaotang.novartc.common.constant.IMConstant;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created on 2026/3/10 11:36
 *
 * @author tangj
 * @description
 */
@Component
@Getter
public class InstanceIdProvider {
    private final String instanceId;

    public InstanceIdProvider() throws UnknownHostException {
        String host = InetAddress.getLocalHost().getHostAddress();
        this.instanceId = host + ":" + IMConstant.port;
    }
}
