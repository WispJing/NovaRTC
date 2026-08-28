package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.server.dto.UpdateAvatarRequest;
import io.github.xiaotang.novartc.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户信息控制器，接口位于 /api/** 下，需登录后访问。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    /**
     * 更新当前登录用户头像，fileId 必须为本人上传的 avatar 类型文件记录。
     */
    @PostMapping("/update-avatar")
    public Map<String, String> updateAvatar(@RequestBody UpdateAvatarRequest request) {
        String avatarUrl = userService.updateAvatar(request.getFileId());
        return Map.of("avatarUrl", avatarUrl);
    }
}
