package io.github.xiaotang.novartc;

import io.github.xiaotang.novartc.config.ChatAccessLog;
import io.github.xiaotang.novartc.netty.core.NovaRTCServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Created on 2026/2/27 16:14
 *
 * @author tangj
 */
@EnableConfigurationProperties
@SpringBootApplication
public class NovaRtcApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(NovaRtcApplication.class, args);
    }

    @Override
    public void run(String... args) {
        final NovaRTCServer server = new NovaRTCServer();
        server.init();
        server.start();
        ChatAccessLog.log.info("NovaRTC IM server is running...");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            ChatAccessLog.log.info(">>>>>>>>> jvm shutdown");
        }));
    }
}
