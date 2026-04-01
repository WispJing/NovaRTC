package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserVO {
    private String userId;
    private String userNum;
    private String username;
    private String email;
    private String avatarUrl;
    private Boolean isAdmin;
    private Boolean imOnline;
    private Boolean adminOnline;
    private List<String> menuCodes;
}
