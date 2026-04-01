package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AdminRiskUserControlRequest {
    private String userId;
    private String scope;
    private String reason;
    private Instant expiresAt;
}
