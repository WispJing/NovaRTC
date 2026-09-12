package io.github.xiaotang.novartc.job;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * NovaRTC 定时任务调度平台启动类。
 * <p>
 * 负责任务的增删改查、启停、手动执行与调度日志；触发时通过 RocketMQ
 * 通知 NovaRTC 服务（novartc-server）执行，并消费执行结果回传。
 *
 * @author xiaotang
 */
@MapperScan("io.github.xiaotang.novartc.job.mapper")
@EnableScheduling
@SpringBootApplication
public class NovaRtcJobApplication {
    private final Logger logger = LoggerFactory.getLogger(NovaRtcJobApplication.class);

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        logger.info("JVM默认时区: {}", TimeZone.getDefault().getID());
    }

    public static void main(String[] args) {
        SpringApplication.run(NovaRtcJobApplication.class, args);
    }
}
