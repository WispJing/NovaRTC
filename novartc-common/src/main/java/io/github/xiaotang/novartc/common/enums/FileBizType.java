package io.github.xiaotang.novartc.common.enums;

import java.util.Locale;

/**
 * 文件业务类型，code 同时作为对象存储中的业务目录名。
 */
public enum FileBizType {
    /**
     * 用户/群头像
     */
    AVATAR("avatar"),
    /**
     * 图片消息
     */
    IMAGE("image"),
    /**
     * 视频消息
     */
    VIDEO("video"),
    /**
     * 语音消息
     */
    AUDIO("audio"),
    /**
     * 文件消息与其他
     */
    FILE("file");

    private final String code;

    FileBizType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * 按 code 解析（忽略大小写），非法值返回 null。
     */
    public static FileBizType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (FileBizType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
