package io.github.xiaotang.novartc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2026/3/2 15:49
 *
 * @author tangj
 * @description
 */
public class ChatMsgLog {
    private static final Logger logger = LoggerFactory.getLogger("im.msg");

    public static void send(String from, String to, String msgId) {
        logger.info("[MSG_SEND] from={} to={} msgId={}", from, to, msgId);
    }

    public static void receive(String from, String to, String msgId) {
        logger.info("[MSG_RECEIVE] from={} to={} msgId={}", from, to, msgId);
    }
}
