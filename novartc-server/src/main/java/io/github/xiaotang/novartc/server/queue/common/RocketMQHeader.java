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
}
