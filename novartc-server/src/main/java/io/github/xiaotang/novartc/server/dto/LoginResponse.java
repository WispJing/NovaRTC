package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/11 14:15
 *
 * @author tangj
 * @description
 */
@Data
public class LoginResponse {
    private String token;

    public LoginResponse(String token) {
        this.token = token;
    }

    public static LoginResponse of(String token) {
        return new LoginResponse(token);
    }
}
