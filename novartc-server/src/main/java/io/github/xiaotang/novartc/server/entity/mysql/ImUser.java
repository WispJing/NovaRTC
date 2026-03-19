package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 用户信息表
 * </p>
 *
 * @author xiaotang
 * @since 2026-03-19
 */
@Getter
@Setter
@ToString
@TableName("im_user")
public class ImUser extends BaseEntity {

    /**
     * 用户编号
     */
    @TableField("user_num")
    private String userNum;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码哈希值
     */
    @TableField("password")
    private String password;

    /**
     * 电子邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 是否管理员：0-否，1-是
     */
    @TableField("is_admin")
    private Boolean isAdmin;
}
