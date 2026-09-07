package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.entity.mysql.ImUser;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.mapper.ImUserMapper;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.util.MinioUrlResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户头像更新服务单元测试，不启动 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private ImFileMapper imFileMapper;

    @Mock
    private ImUserMapper imUserMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId("U1001");
        MinioProperties properties = new MinioProperties();
        properties.setEndpoint("http://localhost:6000");
        properties.setBucket("novartc");
        userService = new UserServiceImpl(imFileMapper, imUserMapper, new MinioUrlResolver(properties));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    private ImFile avatarFile(String uploaderId, String bizType) {
        ImFile imFile = new ImFile();
        imFile.setId("file-1");
        imFile.setUploaderId(uploaderId);
        imFile.setBizType(bizType);
        imFile.setObjectKey("avatar/20260828/a.png");
        imFile.setUrl("http://localhost:6000/novartc/avatar/20260828/a.png");
        return imFile;
    }

    @Test
    void updateAvatarShouldPersistObjectKeyAndReturnResolvedUrl() {
        when(imFileMapper.selectById("file-1")).thenReturn(avatarFile("U1001", "avatar"));
        when(imUserMapper.selectById("U1001")).thenReturn(new ImUser());

        String avatarUrl = userService.updateAvatar("file-1");

        assertThat(avatarUrl).isEqualTo("http://localhost:6000/novartc/avatar/20260828/a.png");
        ArgumentCaptor<ImUser> userCaptor = ArgumentCaptor.forClass(ImUser.class);
        verify(imUserMapper).updateById(userCaptor.capture());
        assertThat(userCaptor.getValue().getAvatarUrl()).isEqualTo("avatar/20260828/a.png");
    }

    @Test
    void updateAvatarShouldRejectBlankFileId() {
        assertThatThrownBy(() -> userService.updateAvatar(" "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件ID不能为空");
    }

    @Test
    void updateAvatarShouldRejectMissingFile() {
        when(imFileMapper.selectById("none")).thenReturn(null);

        assertThatThrownBy(() -> userService.updateAvatar("none"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件记录不存在");
    }

    @Test
    void updateAvatarShouldRejectFileUploadedByOthers() {
        when(imFileMapper.selectById("file-1")).thenReturn(avatarFile("U2002", "avatar"));

        assertThatThrownBy(() -> userService.updateAvatar("file-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权使用该文件作为头像");
    }

    @Test
    void updateAvatarShouldRejectNonAvatarBizType() {
        when(imFileMapper.selectById("file-1")).thenReturn(avatarFile("U1001", "image"));

        assertThatThrownBy(() -> userService.updateAvatar("file-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权使用该文件作为头像");
    }
}
