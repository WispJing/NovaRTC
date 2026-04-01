package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminLoginVO {
    private String userId;
    private String userNum;
    private String username;
    private String avatarUrl;
    private String token;
    private Boolean superAdmin;
    private List<String> menuCodes;
}
