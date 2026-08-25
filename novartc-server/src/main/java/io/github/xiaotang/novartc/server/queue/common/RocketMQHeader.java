package io.github.xiaotang.novartc.server.queue.common;

/**
 * Created on 2026/3/10 15:30
 *
 * @author tangj
 * @description
 */
public interface RocketMQHeader {
    String MSG_ID = "msgId";
    String TARGET_USER_ID = "targetUserId";
    String SOURCE_INSTANCE_ID = "sourceInstanceId";
    /**
     * 路由表未命中时的兜底消息标签：所有实例均订阅该标签，收到后按本地连接过滤投递。
     */
    String FALLBACK_TAG = "novartc-fallback";
}
