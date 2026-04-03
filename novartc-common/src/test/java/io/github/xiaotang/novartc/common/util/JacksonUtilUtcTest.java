package io.github.xiaotang.novartc.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonUtilUtcTest {

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

    @Test
    void shouldRoundTripInstantWithoutTimeZoneDrift() {
        Instant expected = Instant.parse("2026-04-03T12:34:56Z");

        String json = JacksonUtil.toJson(Map.of("time", expected));
        Map<String, Instant> result = JacksonUtil.toObject(json, new TypeReference<>() {});

        assertEquals(expected, result.get("time"));
    }
}
