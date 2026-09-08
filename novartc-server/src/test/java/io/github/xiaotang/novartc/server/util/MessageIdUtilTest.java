package io.github.xiaotang.novartc.server.util;

import io.github.xiaotang.novartc.common.util.MessageIdUtil;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageIdUtilTest {
    private static final long MESSAGE_ID = 351556960335372076L;

    private final MessageIdUtil messageIdUtil = new MessageIdUtil(0, 0);

    @Test
    void resolveMessageId() {
        assertAll(
                () -> assertEquals(1787884910956L, messageIdUtil.extractTimestamp(MESSAGE_ID)),
                () -> assertEquals(13L, messageIdUtil.extractDatacenterId(MESSAGE_ID)),
                () -> assertEquals(17L, messageIdUtil.extractWorkerId(MESSAGE_ID)),
                () -> assertEquals(3884L, messageIdUtil.extractSequence(MESSAGE_ID))
        );
        System.out.println(messageIdUtil.formatId(MESSAGE_ID));
    }
}
