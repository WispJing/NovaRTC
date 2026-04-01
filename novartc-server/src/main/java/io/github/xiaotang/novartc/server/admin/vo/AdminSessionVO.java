package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

@Data
public class AdminSessionVO {
    private String userId;
    private String userNum;
    private String username;
    private String token;
    private String clientType;
    private Long loginTime;
    private Boolean online;
    private String source;
}
