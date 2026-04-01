package io.github.xiaotang.novartc.common.constant;

/**
 * Created on 2026/3/18 15:31
 *
 * @author tangj
 * @description
 */
public interface LoginConstant {
    String TOKEN_PREFIX = "login:token:";
    String USER_TOKEN_PREFIX = "login:user:";

    String ADMIN_TOKEN_PREFIX = "admin:token:";
    String ADMIN_USER_TOKEN_PREFIX = "admin:user:";

    // 30分钟（滑动过期）
    long TOKEN_TTL = 300 * 60;
    long ADMIN_TOKEN_TTL = 300 * 60;
}
