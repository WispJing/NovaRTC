package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.common.util.UserAgentUtil;
import io.github.xiaotang.novartc.server.dto.IMUserDTO;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created on 2026/3/11 9:11
 *
 * @author tangj
 * @description
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class LoginController {
    private final LoginService loginService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody IMUserDTO imUserDTO,
                               HttpServletRequest request) {
        String clientType = imUserDTO.getClientType();
        if (StringUtils.isBlank(clientType)) {
            clientType = UserAgentUtil.parseClientTypeFromUserAgent(request.getHeader("User-Agent"));
        }
        return loginService.login(imUserDTO.getUserNum(), imUserDTO.getPassword(), clientType);
    }

    @PostMapping("/logout")
    public void logout(@RequestBody LoginResponse loginResponse) {
        loginService.logout(loginResponse.getToken());
    }
}
