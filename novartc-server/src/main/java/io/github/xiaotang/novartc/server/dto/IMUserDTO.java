package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/11 13:41
 *
 * @author tangj
 * @description
 */
@Data
public class IMUserDTO {
    private String userNum;
    private String username;
    private String password;
    private String clientType;
}
