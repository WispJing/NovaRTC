package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * 注册请求，承载新用户注册所需的基础信息。
 */
@Data
public class RegisterDTO {
    private String userNum;
    private String username;
    private String password;
    private String email;
}
