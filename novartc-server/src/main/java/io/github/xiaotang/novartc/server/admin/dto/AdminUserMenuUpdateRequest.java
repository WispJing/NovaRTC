package io.github.xiaotang.novartc.server.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserMenuUpdateRequest {
    private List<String> menuIds;
}
