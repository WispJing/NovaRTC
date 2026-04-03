package io.github.xiaotang.novartc.common.constant;

/**
 * Created on 2026/3/18 15:31
 *
 * @author tangj
 * @description
 */
public interface LoginConstant {
    /**
     * 登录作用域，用于区分 IM 端、后台端以及全局控制。
     */
    String LOGIN_SCOPE_IM = "IM";
    String LOGIN_SCOPE_ADMIN = "ADMIN";
    String LOGIN_SCOPE_ALL = "ALL";

    /**
     * 后台登录类型与会话来源标识。
     */
    String LOGIN_TYPE_ADMIN = "ADMIN";
    String SESSION_SOURCE_IM = "IM";
    String SESSION_SOURCE_ADMIN = "ADMIN";

    /**
     * 后台未显式指定客户端类型时的默认值。
     */
    String DEFAULT_ADMIN_CLIENT_TYPE = "admin-web";

    /**
     * Redis token Hash 中的标准字段名。
     */
    String TOKEN_INFO_USER_ID = "userId";
    String TOKEN_INFO_CLIENT_TYPE = "clientType";
    String TOKEN_INFO_LOGIN_TIME = "loginTime";

    /**
     * IM/后台 token 与用户会话集合的 Redis Key 前缀。
     */
    String TOKEN_PREFIX = "login:token:";
    String USER_TOKEN_PREFIX = "login:user:";

    String ADMIN_TOKEN_PREFIX = "admin:token:";
    String ADMIN_USER_TOKEN_PREFIX = "admin:user:";

    /**
     * Token 过期时间，单位秒，采用滑动续期。
     */
    long TOKEN_TTL = 300 * 60;
    long ADMIN_TOKEN_TTL = 300 * 60;
}
