package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AdminRiskGroupMuteRequest {
    private String groupId;
    private String userId;
    private String reason;
    private Instant expiresAt;
}
