package io.github.xiaotang.novartc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.github.xiaotang.novartc.config.ChatAppLog;
import io.github.xiaotang.novartc.netty.NovaRTCServer;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.TimeZone;

/**
 * Created on 2026/2/27 16:14
 *
 * @author tangj
 */
@EnableMongoAuditing
@EnableConfigurationProperties
@SpringBootApplication
public class NovaRtcApplication implements CommandLineRunner {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
        SpringApplication.run(NovaRtcApplication.class, args);
        JSONObject.defaultTimeZone = TimeZone.getTimeZone("UTC");
    }

    @Override
    public void run(String... args) {
        final NovaRTCServer server = new NovaRTCServer();
        server.init();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            ChatAppLog.log.info("NovaRTC IM server is stopping");
        }));
    }
}
