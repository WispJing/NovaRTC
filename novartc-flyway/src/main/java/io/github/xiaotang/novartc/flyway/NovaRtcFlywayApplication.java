package io.github.xiaotang.novartc.flyway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Flyway 独立启动入口，只负责执行数据库版本迁移。
 */
@SpringBootApplication
public class NovaRtcFlywayApplication implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NovaRtcFlywayApplication.class);

    private final ConfigurableApplicationContext applicationContext;
    private final Environment environment;

    public NovaRtcFlywayApplication(ConfigurableApplicationContext applicationContext, Environment environment) {
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NovaRtcFlywayApplication.class);
        application.setLogStartupInfo(true);
        ConfigurableApplicationContext context = application.run(args);
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Flyway 数据版本管理执行完成: schemaHistoryTable={}, locations={}",
                environment.getProperty("spring.flyway.table"),
                environment.getProperty("spring.flyway.locations"));
    }
}
