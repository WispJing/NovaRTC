package io.github.xiaotang.novartc;

import io.github.xiaotang.novartc.repository.mongodb.IMUserRepository;
import io.github.xiaotang.novartc.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final Logger logger = LoggerFactory.getLogger(NovaRtcApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(NovaRtcApplication.class, args);
    }

    @Override
    public void run(String... args) {
        final NovaRTCServer server = new NovaRTCServer();
        server.init();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            logger.info(">>>>>>>>> jvm shutdown");
        }));
    }
}
