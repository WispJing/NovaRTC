package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.IMUserDTO;
import io.github.xiaotang.novartc.server.dto.LoginResponse;

/**
 * Created on 2026/3/11 13:43
 *
 * @author tangj
 * @description
 */
public interface ImUserService {
    LoginResponse login(IMUserDTO imUserDTO);
}
