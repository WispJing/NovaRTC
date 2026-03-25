package io.github.xiaotang.novartc.common.enums;

/**
 * Created on 2026/3/25
 *
 * @author tangj
 * @description 好友申请状态
 */
public enum FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED;

    public boolean isFinalStatus() {
        return this == ACCEPTED || this == REJECTED || this == CANCELED;
    }
}
