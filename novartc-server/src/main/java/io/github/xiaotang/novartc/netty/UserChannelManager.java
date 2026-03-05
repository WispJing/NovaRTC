package io.github.xiaotang.novartc.netty;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 2026/3/5 14:04
 *
 * @author tangj
 * @description
 */
public class UserChannelManager {
    private static final ConcurrentHashMap<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    public static void bind(String userId, Channel channel) {
        USER_CHANNEL_MAP.put(userId, channel);
    }

    public static Channel getChannel(String userId) {
        return USER_CHANNEL_MAP.get(userId);
    }

    public static void remove(String userId) {
        USER_CHANNEL_MAP.remove(userId);
    }
}
