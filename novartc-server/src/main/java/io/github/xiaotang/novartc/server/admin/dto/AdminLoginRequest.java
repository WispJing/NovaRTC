package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String userNum;
    private String password;
    private String clientType;
}
