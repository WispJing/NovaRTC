package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * Created on 2026/3/24 12:12
 *
 * @author tangj
 * @description
 */
@Data
public class StartChatRequest {
    private String targetUserId;
}
