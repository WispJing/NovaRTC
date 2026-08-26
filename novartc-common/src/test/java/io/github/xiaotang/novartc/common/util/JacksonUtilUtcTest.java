package io.github.xiaotang.novartc.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JacksonUtil 序列化测试，验证时间类型统一按 UTC 序列化，
 * 不受机器默认时区影响。
 */
class JacksonUtilUtcTest {

    /**
     * 即使默认时区被改为 Asia/Shanghai，旧的 Date 类型仍按 UTC（+00:00）输出。
     */
    @Test
    void shouldSerializeLegacyDateTypesInUtcEvenWhenDefaultTimeZoneIsNotUtc() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));

            String json = JacksonUtil.toJson(Map.of("time", Date.from(Instant.parse("2026-04-03T00:00:00Z"))));

            assertTrue(json.contains("2026-04-03T00:00:00.000+00:00"), json);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * Instant 序列化后再反序列化，值与原时间完全一致，无时区漂移。
     */
    @Test
    void shouldRoundTripInstantWithoutTimeZoneDrift() {
        Instant expected = Instant.parse("2026-04-03T12:34:56Z");

        String json = JacksonUtil.toJson(Map.of("time", expected));
        Map<String, Instant> result = JacksonUtil.toObject(json, new TypeReference<>() {});

        assertEquals(expected, result.get("time"));
    }
}
