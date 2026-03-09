package io.github.xiaotang.novartc.server.config;

import io.github.xiaotang.novartc.server.netty.MessageLogConverter;
import io.github.xiaotang.novartc.common.util.JacksonUtil;
import io.github.xiaotang.novartc.server.proto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2026/3/2 15:49
 *
 * @author tangj
 * @description
 */
public class ChatMsgLog {
    public static final Logger log = LoggerFactory.getLogger("im.msg");

    public static void msg(ChatMessage.MsgBody msg) {
        log.info("{}", JacksonUtil.toJson(MessageLogConverter.convert(msg)));
    }
}
