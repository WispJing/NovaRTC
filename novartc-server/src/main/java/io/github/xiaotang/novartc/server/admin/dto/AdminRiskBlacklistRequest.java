package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

@Data
public class AdminRiskBlacklistRequest {
    private String ownerUserId;
    private String blockedUserId;
    private String reason;
}
