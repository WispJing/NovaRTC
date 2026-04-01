package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

@Data
public class AdminKickClientRequest {
    private String userId;
    private String clientType;
}
