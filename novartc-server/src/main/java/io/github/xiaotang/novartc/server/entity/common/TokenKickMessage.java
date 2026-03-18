package io.github.xiaotang.novartc.server.entity.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created on 2026/3/18 17:41
 *
 * @author tangj
 * @description
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenKickMessage {

    private String userId;
    private String token;
}
