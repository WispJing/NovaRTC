package io.github.xiaotang.novartc.config;

import io.github.xiaotang.novartc.netty.MessageLogConverter;
import io.github.xiaotang.novartc.proto.ChatMessage;
import io.github.xiaotang.novartc.util.JacksonUtil;
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
