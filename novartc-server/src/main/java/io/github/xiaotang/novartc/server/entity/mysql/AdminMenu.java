package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 后台菜单表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("admin_menu")
public class AdminMenu extends BaseEntity {

    /**
     * 父级菜单ID，顶级为空字符串
     */
    @TableField("parent_id")
    private String parentId;

    /**
     * 菜单编码
     */
    @TableField("menu_code")
    private String menuCode;

    /**
     * 菜单名称
     */
    @TableField("menu_name")
    private String menuName;

    /**
     * 菜单类型：CATALOG/PAGE/BUTTON
     */
    @TableField("menu_type")
    private String menuType;

    /**
     * 路由路径
     */
    @TableField("path")
    private String path;

    /**
     * 前端组件路径
     */
    @TableField("component")
    private String component;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 排序号
     */
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 状态：0-禁用，1-启用
     */
    @TableField("status")
    private Byte status;

    /**
     * 是否显示：0-否，1-是
     */
    @TableField("visible")
    private Byte visible;
}
