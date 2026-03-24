package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/24 12:12
 *
 * @author tangj
 * @description
 */
@Data
public class ContactVO {
    private String userId;
    private String userNum;
    private String username;
    private String avatarUrl;
    private Boolean online;
}
