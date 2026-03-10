package io.github.xiaotang.novartc.common.constant;

/**
 * Created on 2026/2/28 13:50
 *
 * @author tangj
 * @description
 */
public class IMConstant {
    public static String host = "localhost";
    public static int port = 9688;
    public static String websocketUrl = "ws://localhost:" + port + "/ws";

    public static final String USER_INSTANCES_KEY_PREFIX = "im:user:instances:";
    public static final String INSTANCE_KEY_PREFIX = "im:user:instance:";
    public static final String REDIS_GROUP_MEMBERS_PREFIX = "im:group:members:";
}
