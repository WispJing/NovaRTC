package io.github.xiaotang.novartc.common.constant;

/**
 * 风控相关常量。
 */
public interface RiskConstant {
    /**
     * 用户级风控控制类型。
     */
    String USER_CONTROL_LOGIN_BAN = "LOGIN_BAN";
    String USER_CONTROL_SEND_MUTE = "SEND_MUTE";

    /**
     * 群禁言范围定义。
     */
    String GROUP_MUTE_SCOPE_ALL = "GROUP_ALL";
    String GROUP_MUTE_SCOPE_USER = "USER";

    /**
     * 黑名单来源标识。
     */
    String BLACKLIST_SOURCE_MANUAL = "MANUAL";

    /**
     * 登录风控告警类型。
     */
    String ALERT_TYPE_LOGIN_BANNED = "LOGIN_BANNED";
    String ALERT_TYPE_PASSWORD_ERROR = "PASSWORD_ERROR";
    String ALERT_TYPE_MULTI_CLIENT = "MULTI_CLIENT";

    /**
     * 风控告警等级。
     */
    String ALERT_LEVEL_LOW = "LOW";
    String ALERT_LEVEL_MEDIUM = "MEDIUM";
    String ALERT_LEVEL_HIGH = "HIGH";

    /**
     * 风控缓存相关 Redis Key 前缀。
     */
    String REDIS_PREFIX = "risk:";
    String USER_CONTROL_PREFIX = "risk:user:controls:";
    String GROUP_ALL_MUTE_PREFIX = "risk:group:mutes:all:";
    String GROUP_USER_MUTE_PREFIX = "risk:group:mutes:user:";
    String GROUP_USER_MUTE_INDEX_PREFIX = "risk:group:mutes:index:";
    String BLACKLIST_PREFIX = "risk:blacklist:";
}
