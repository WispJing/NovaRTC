package io.github.xiaotang.novartc.server.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置，复用 spring.data.redis 连接参数，
 * 为分布式锁等场景提供 {@link RedissonClient}。
 */
@Configuration
public class RedissonConfig {

    /**
     * Redisson 单机模式客户端，应用关闭时释放。
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.data.redis.host}") String host,
                                         @Value("${spring.data.redis.port}") Integer port,
                                         @Value("${spring.data.redis.database:0}") Integer database) {
        Config config = new Config();
        // Redisson 地址需带 scheme 前缀
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database);
        return Redisson.create(config);
    }
}
