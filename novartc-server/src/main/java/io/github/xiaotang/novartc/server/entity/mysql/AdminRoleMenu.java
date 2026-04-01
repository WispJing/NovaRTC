package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 后台角色菜单关系表
 * </p>
 *
 * @author xiaotang
 * @since 2026-04-01
 */
@Getter
@Setter
@ToString
@TableName("admin_role_menu")
public class AdminRoleMenu extends BaseEntity {

    /**
     * 角色ID
     */
    @TableField("role_id")
    private String roleId;

    /**
     * 菜单ID
     */
    @TableField("menu_id")
    private String menuId;
}
