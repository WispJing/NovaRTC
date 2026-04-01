package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserMenuVO {
    private String userId;
    private List<String> menuIds;
    private List<String> menuCodes;
}
