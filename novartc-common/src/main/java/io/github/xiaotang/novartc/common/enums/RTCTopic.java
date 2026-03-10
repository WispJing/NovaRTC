package io.github.xiaotang.novartc.common.enums;

import lombok.Getter;

/**
 * Created on 2026/3/10 9:29
 *
 * @author tangj
 * @description
 */
@Getter
public enum RTCTopic {
    PRIVATE_MSG("im_private_msg"),
    GROUP_MSG("im_group_msg"),
    OFFLINE_MSG("im_offline_msg");

    private final String topic;

    RTCTopic(String topic) {
        this.topic = topic;
    }
}
