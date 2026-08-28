package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.enums.FileBizType;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.mapper.ImUserMapper;
import io.github.xiaotang.novartc.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户信息服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final ImFileMapper imFileMapper;
    private final ImUserMapper imUserMapper;

    @Override
    public String updateAvatar(String fileId) {
        String userId = UserContextHolder.getUserId();
        if (StringUtils.isBlank(fileId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "文件ID不能为空");
        }

        // 仅允许使用本人上传的 avatar 类型文件，防止任意 URL 注入
        ImFile imFile = imFileMapper.selectById(fileId);
        if (imFile == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "文件记录不存在");
        }
        if (!Objects.equals(imFile.getUploaderId(), userId)
                || !FileBizType.AVATAR.code().equals(imFile.getBizType())) {
            throw new BusinessException(ResultCode.FORBIDDEN.code(), "无权使用该文件作为头像");
        }

        ImUser imUser = imUserMapper.selectById(userId);
        if (imUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        imUser.setAvatarUrl(imFile.getUrl());
        imUserMapper.updateById(imUser);
        logger.info("用户头像更新成功: 用户={}, 文件={}, 地址={}", userId, fileId, imFile.getUrl());
        return imFile.getUrl();
    }
}
