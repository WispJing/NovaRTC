package io.github.xiaotang.novartc.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

/**
 * Created on 2026/2/27 16:43
 *
 * @author tangj
 * @description
 */
public class JacksonUtil {
    
    private static ObjectMapper objectMapper;

    private static ObjectMapper getJackson() {
        if (objectMapper == null) {
            synchronized (JacksonUtil.class) {
                if (objectMapper == null) {
                    objectMapper = new ObjectMapper();
                }
            }
        }
        return objectMapper;
    }

    static {
        // 注册 Java8 时间支持（LocalDateTime 等）
        getJackson().registerModule(new JavaTimeModule());

        // 时间戳格式关闭
        getJackson().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知字段
        getJackson().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 空对象不报错
        getJackson().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // null 字段不序列化
        getJackson().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JacksonUtil() {
    }

    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return getJackson().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Object to JSON error", e);
        }
    }

    /**
     * JSON 转对象
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return getJackson().readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON to Object error", e);
        }
    }

    /**
     * JSON 转复杂泛型对象
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        try {
            return getJackson().readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("JSON to Object error", e);
        }
    }

    /**
     * 获取底层 ObjectMapper（高级用法）
     */
    public static ObjectMapper getMapper() {
        return getJackson();
    }
}
