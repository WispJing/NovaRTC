package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.common.job.JobInvokeMessage;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.config.JobMqProperties;
import io.github.xiaotang.novartc.server.task.JobInvokeExecutor;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 定时任务执行通知消费者：订阅调度平台的任务通知 topic，
 * 集群模式下组内仅一个实例消费，按消息中的执行类、方法、参数反射执行。
 *
 * @author xiaotang
 */
@Configuration
public class RocketMQJobConsumerConfig {
    private final Logger logger = LoggerFactory.getLogger(RocketMQJobConsumerConfig.class);

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Autowired
    private JobMqProperties jobMqProperties;
    @Autowired
    private JobInvokeExecutor jobInvokeExecutor;

    private DefaultMQPushConsumer jobConsumer;

    @Bean
    public DefaultMQPushConsumer jobNotifyConsumer() throws Exception {
        jobConsumer = new DefaultMQPushConsumer(jobMqProperties.getConsumerGroup());
        jobConsumer.setNamesrvAddr(nameServer);
        jobConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        // 集群模式：同组多实例时仅一个实例执行任务，避免重复执行
        jobConsumer.setMessageModel(MessageModel.CLUSTERING);
        jobConsumer.subscribe(jobMqProperties.getTopic(), "*");
        logger.info("RocketMQ 定时任务消费者订阅完成: topic={}, group={}",
                jobMqProperties.getTopic(), jobMqProperties.getConsumerGroup());

        jobConsumer.registerMessageListener((MessageListenerConcurrently) (msgList, context) -> {
            for (MessageExt msg : msgList) {
                try {
                    JobInvokeMessage message = JacksonUtil.toObject(
                            new String(msg.getBody(), StandardCharsets.UTF_8), JobInvokeMessage.class);
                    logger.info("收到定时任务执行通知: logId={}, jobId={}, jobName={}, triggerType={}, target={}#{}",
                            message.getLogId(), message.getJobId(), message.getJobName(),
                            message.getTriggerType(), message.getInvokeClass(), message.getInvokeMethod());
                    jobInvokeExecutor.handle(message);
                } catch (Exception e) {
                    logger.error("RocketMQ 定时任务通知消费失败", e);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        jobConsumer.start();
        logger.info("RocketMQ 定时任务消费者已启动");
        return jobConsumer;
    }

    @PreDestroy
    public void destroy() {
        if (jobConsumer != null) {
            jobConsumer.shutdown();
            logger.info("RocketMQ 定时任务消费者已关闭");
        }
    }
}
