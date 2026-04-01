package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.time.Instant;

@Data
public class RiskLoginAlertVO {
    private String id;
    private String userId;
    private String userNum;
    private String username;
    private String clientType;
    private String ip;
    private String userAgent;
    private String alertType;
    private String alertLevel;
    private String detail;
    private Byte handledStatus;
    private String handlerUserId;
    private String handlerName;
    private Instant handledAt;
    private Instant createdAt;
}
