package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class RiskGroupMuteVO {
    private String id;
    private String groupId;
    private String groupNum;
    private String groupName;
    private String userId;
    private String userNum;
    private String username;
    private String muteScope;
    private String reason;
    private Instant expiresAt;
    private Byte status;
    private Boolean active;
    private Instant createdAt;
}
