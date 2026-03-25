package io.github.xiaotang.novartc.server.queue.rocketmq;

import io.github.xiaotang.novartc.server.entity.common.TokenKickMessage;
import io.github.xiaotang.novartc.server.netty.UserChannelManager;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created on 2026/3/18 17:40
 *
 * @author tangj
 * @description
 */
@Component
@RocketMQMessageListener(topic = "${im.tokenKickTopic}", consumerGroup = "${im.tokenKickConsumerGroup}")
public class TokenKickConsumer implements RocketMQListener<TokenKickMessage> {
    @Autowired
    private UserChannelManager userChannelManager;

    @Override
    public void onMessage(TokenKickMessage message) {
        // 关闭本实例上该 token 对应的连接
        userChannelManager.closeByToken(message.getToken());
    }
}
