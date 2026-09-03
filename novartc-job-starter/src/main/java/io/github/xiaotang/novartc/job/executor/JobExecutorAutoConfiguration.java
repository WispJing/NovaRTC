package io.github.xiaotang.novartc.job.executor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.apache.rocketmq.spring.core.RocketMQTemplate;

/** 自动装配任务消费端；未配置服务名时不会创建消费者。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JobExecutorProperties.class)
@ConditionalOnProperty(prefix = "novartc.job.executor", name = "service-name")
public class JobExecutorAutoConfiguration {
    @Bean
    public JobHandlerRegistry jobHandlerRegistry(ApplicationContext context) {
        return new JobHandlerRegistry(context);
    }

    @Bean
    public JobInvokeExecutor jobInvokeExecutor(JobHandlerRegistry registry,
                                               RocketMQTemplate template,
                                               JobExecutorProperties properties) {
        return new JobInvokeExecutor(registry, template, properties);
    }

    @Bean
    public RocketMqJobConsumer jobExecutorConsumer(JobInvokeExecutor executor, JobExecutorProperties properties) {
        return new RocketMqJobConsumer(executor, properties);
    }
}
