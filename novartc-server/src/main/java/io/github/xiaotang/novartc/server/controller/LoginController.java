package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.server.dto.IMUserDTO;
import io.github.xiaotang.novartc.server.dto.LoginResponse;
import io.github.xiaotang.novartc.server.service.IImUserService;
import io.github.xiaotang.novartc.server.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created on 2026/3/11 9:11
 *
 * @author tangj
 * @description
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/im-user")
public class LoginController {
    private final LoginService loginService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody IMUserDTO imUserDTO) {
        return loginService.login(imUserDTO.getUserNum(), imUserDTO.getPassword());
    }

}
