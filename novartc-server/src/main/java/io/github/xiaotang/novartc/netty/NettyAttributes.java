package io.github.xiaotang.novartc.netty;

import io.netty.util.AttributeKey;

/**
 * Created on 2026/3/5 14:03
 *
 * @author tangj
 * @description
 */
public class NettyAttributes {
    // 用户ID
    public static final AttributeKey<String> USER_ID = AttributeKey.valueOf("userId");

    // 用户名
    public static final AttributeKey<String> USERNAME = AttributeKey.valueOf("username");
}
