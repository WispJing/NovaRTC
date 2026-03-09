package io.github.xiaotang.novartc.server.netty;

import io.netty.channel.Channel;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 2026/3/5 14:04
 *
 * @author tangj
 * @description
 */
public class UserChannelManager {
    private static final ConcurrentHashMap<String, Set<Channel>> USER_CHANNELS = new ConcurrentHashMap<>();

    public static void bind(String userId, Channel channel) {
        USER_CHANNELS
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(channel);
    }

    public static void send(String userId, Object msg) {
        Set<Channel> channels = USER_CHANNELS.get(userId);

        if (channels == null) {
            return;
        }
        for (Channel ch : channels) {
            if (ch.isActive()) {
                ch.writeAndFlush(msg);
            }
        }
    }

    public static Set<Channel> getChannel(String userId) {
        return USER_CHANNELS.get(userId);
    }

    public static void remove(String userId, Channel channel) {
        if (StringUtils.isBlank(userId)) {
            return;
        }
        Set<Channel> channels = USER_CHANNELS.get(userId);
        if (channels == null) {
            return;
        }
        channels.remove(channel);

        if (channels.isEmpty()) {
            USER_CHANNELS.remove(userId);
        }
    }
}
