package io.github.xiaotang.novartc.server.dto;

import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created on 2026/3/11 14:15
 *
 * @author tangj
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String userId;
    /**
     * 用户编号
     */
    private String userNum;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码哈希值
     */
    private String password;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatarUrl;

    private String token;

    public LoginResponse(String token) {
        this.token = token;
    }

    public static LoginResponse of(String token) {
        return new LoginResponse(token);
    }

    public LoginResponse(ImUser imUser, String token) {
        this.userId = imUser.getId();
        this.userNum = imUser.getUserNum();
        this.username = imUser.getUsername();
        this.avatarUrl = imUser.getAvatarUrl();
        this.token = token;
    }

    public static LoginResponse of(ImUser imUser, String token) {
        return new LoginResponse(imUser, token);
    }
}
