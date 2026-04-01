package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class RiskBlacklistVO {
    private String id;
    private String ownerUserId;
    private String ownerUserNum;
    private String ownerUsername;
    private String blockedUserId;
    private String blockedUserNum;
    private String blockedUsername;
    private String source;
    private String reason;
    private Byte status;
    private Instant createdAt;
}
