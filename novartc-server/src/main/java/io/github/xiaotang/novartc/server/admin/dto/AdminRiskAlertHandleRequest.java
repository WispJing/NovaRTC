package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

@Data
public class AdminRiskAlertHandleRequest {
    private Byte handledStatus = 1;
}
