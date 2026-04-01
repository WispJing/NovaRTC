package io.github.xiaotang.novartc.server.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminMenuVO {
    private String id;
    private String parentId;
    private String menuCode;
    private String menuName;
    private String menuType;
    private String path;
    private String component;
    private String icon;
    private Integer sortNo;
    private List<AdminMenuVO> children = new ArrayList<>();
}
