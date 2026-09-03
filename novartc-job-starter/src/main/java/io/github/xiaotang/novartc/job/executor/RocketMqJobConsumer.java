package io.github.xiaotang.novartc.job.executor;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class RocketMqJobConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RocketMqJobConsumer.class);

    private final JobInvokeExecutor executor;
    private final JobExecutorProperties properties;
    private final AtomicInteger threadSequence = new AtomicInteger();
    private final ThreadLocal<Boolean> threadNamed = new ThreadLocal<>();

    @Value("${rocketmq.name-server}")
    private String nameServer;

    private DefaultMQPushConsumer consumer;

    public RocketMqJobConsumer(JobInvokeExecutor executor, JobExecutorProperties properties) {
        this.executor = executor;
        this.properties = properties;
    }

    @PostConstruct
    public void start() throws Exception {
        String consumerGroup = StringUtils.defaultIfBlank(
                properties.getConsumerGroup(),
                "novartc-job-executor-" + properties.getServiceName());
        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        consumer.setMessageModel(MessageModel.CLUSTERING);
        // Tag 与目标服务名完全一致，保证一条任务只会被目标服务消费。
        consumer.subscribe(properties.getTopic(), properties.getServiceName());
        consumer.registerMessageListener(
                (org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently)
                        (messages, context) -> consume(messages));
        consumer.start();
        logger.info("任务消费者已启动: topic={}, tag={}, group={}",
                properties.getTopic(), properties.getServiceName(), consumerGroup);
    }

    private ConsumeConcurrentlyStatus consume(
            java.util.List<org.apache.rocketmq.common.message.MessageExt> messages) {
        renameConsumerThreadIfNecessary();
        try {
            for (var raw : messages) {
                String body = new String(raw.getBody(), StandardCharsets.UTF_8);
                executor.handle(JacksonUtil.toObject(body, JobInvokeMessage.class));
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Exception e) {
            logger.error("任务消息消费失败，将请求 RocketMQ 重试", e);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

    /**
     * RocketMQ 默认线程名通常会包含较长的服务标识。仅重命名本消费者实际执行任务的
     * 工作线程，方便日志阅读；服务名、Tag 和消费组仍使用完整配置值。
     */
    private void renameConsumerThreadIfNecessary() {
        if (Boolean.TRUE.equals(threadNamed.get())) {
            return;
        }
        String prefix = StringUtils.defaultIfBlank(
                properties.getThreadNamePrefix(), "job-exec-");
        Thread.currentThread().setName(prefix + threadSequence.incrementAndGet());
        threadNamed.set(Boolean.TRUE);
    }

    @PreDestroy
    public void stop() {
        if (consumer != null) {
            consumer.shutdown();
            logger.info("任务消费者已关闭: serviceName={}", properties.getServiceName());
        }
    }
}
