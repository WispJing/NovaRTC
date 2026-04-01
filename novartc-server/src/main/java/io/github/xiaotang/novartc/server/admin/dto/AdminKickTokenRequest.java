package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

@Data
public class AdminKickTokenRequest {
    private String userId;
    private String token;
}
