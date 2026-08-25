package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.common.util.MD5Util;
import io.github.xiaotang.novartc.common.util.StringUtil;
import io.github.xiaotang.novartc.server.dto.RegisterDTO;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 注册服务，负责 IM 新用户的入参校验、唯一性校验与用户创建。
 */
@Service
@RequiredArgsConstructor
public class RegisterService {

    /**
     * 邮箱格式校验
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 密码最小长度
     */
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final IImUserService imUserService;

    /**
     * 注册新用户，用户编号与邮箱均要求唯一。
     */
    public void register(RegisterDTO dto) {
        validate(dto);
        if (imUserService.selectByUserNum(dto.getUserNum()) != null) {
            throw new BusinessException(ResultCode.REGISTER_USER_NUM_EXISTS);
        }
        if (imUserService.selectByEmail(dto.getEmail()) != null) {
            throw new BusinessException(ResultCode.REGISTER_EMAIL_EXISTS);
        }

        ImUser user = new ImUser();
        user.setId(StringUtil.uuid32());
        user.setUserNum(dto.getUserNum());
        user.setUsername(dto.getUsername());
        user.setPassword(MD5Util.md5(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setIsAdmin(false);
        user.setIsDeleted(false);
        imUserService.save(user);
    }

    /**
     * 注册入参基础校验。
     */
    private void validate(RegisterDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        if (StringUtils.isBlank(dto.getUserNum())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "用户编号不能为空");
        }
        if (StringUtils.isBlank(dto.getUsername())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "用户名不能为空");
        }
        if (StringUtils.isBlank(dto.getPassword()) || dto.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "密码长度不能少于6位");
        }
        if (StringUtils.isBlank(dto.getEmail()) || !EMAIL_PATTERN.matcher(dto.getEmail()).matches()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "邮箱格式不正确");
        }
    }
}
