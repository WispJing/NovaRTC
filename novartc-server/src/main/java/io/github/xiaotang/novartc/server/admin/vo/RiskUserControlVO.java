package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class RiskUserControlVO {
    private String id;
    private String userId;
    private String userNum;
    private String username;
    private String controlType;
    private String scope;
    private String reason;
    private LocalDateTime expiresAt;
    private Byte status;
    private Boolean active;
    private Instant createdAt;
}
