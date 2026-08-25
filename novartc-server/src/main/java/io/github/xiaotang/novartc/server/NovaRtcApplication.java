package io.github.xiaotang.novartc.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.github.xiaotang.novartc.server.admin.service.RiskControlCacheService;
import io.github.xiaotang.novartc.server.netty.NovaRTCServer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@MapperScan("io.github.xiaotang.novartc.server.mapper")
@EnableMongoAuditing
@EnableConfigurationProperties
@SpringBootApplication
@RequiredArgsConstructor
public class NovaRtcApplication implements CommandLineRunner {
    private final Logger logger = LoggerFactory.getLogger(NovaRtcApplication.class);
    private final NovaRTCServer novaRtcServer;
    private final RiskControlCacheService riskControlCacheService;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        logger.info("JVM默认时区: {}", TimeZone.getDefault().getID());
    }

    public static void main(String[] args) {
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
        SpringApplication.run(NovaRtcApplication.class, args);
        JSONObject.defaultTimeZone = TimeZone.getTimeZone("UTC");
    }

    @Override
    public void run(String... args) {
        riskControlCacheService.warmupAllCaches();
        novaRtcServer.init();
        novaRtcServer.start();
        // 关停由 ImGracefulShutdownService 统一编排（排空→通知→断开→关闭），不再注册 JVM shutdownHook
    }
}
