package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.enums.FileBizType;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.dto.FileUploadResponse;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.util.MinioUtil;
import io.github.xiaotang.novartc.server.util.MinioUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * MinIO 文件存储服务单元测试，不启动 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceImplTest {

    @Mock
    private MinioUtil minioUtil;

    @Mock
    private ImFileMapper imFileMapper;

    private MinioProperties minioProperties;

    private MinioFileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        minioProperties = new MinioProperties();
        minioProperties.setEndpoint("http://localhost:6000");
        minioProperties.setBucket("novartc");
        minioProperties.setMaxFileSize(1L);
        minioProperties.setAllowedExtensions(List.of("jpg", "png", "pdf"));
        fileStorageService = new MinioFileStorageServiceImpl(
                minioProperties, minioUtil, imFileMapper, new MinioUrlResolver(minioProperties));
    }

    @Test
    void storeShouldUploadToBizDirAndRegisterFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.PNG", "image/png", new byte[]{1, 2, 3});

        FileUploadResponse response = fileStorageService.store(file, "avatar");

        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(minioUtil).putObject(eq("novartc"), objectNameCaptor.capture(),
                any(InputStream.class), eq("image/png"), anyLong());

        String dateDir = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(objectNameCaptor.getValue()).startsWith("avatar/" + dateDir + "/");
        assertThat(objectNameCaptor.getValue()).endsWith(".png");

        assertThat(response.getBizType()).isEqualTo("avatar");
        assertThat(response.getOriginalName()).isEqualTo("avatar.PNG");
        assertThat(response.getSize()).isEqualTo(3L);
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getFileId()).isNotBlank();
        assertThat(response.getUrl())
                .isEqualTo("http://localhost:6000/novartc/" + objectNameCaptor.getValue());

        ArgumentCaptor<ImFile> imFileCaptor = ArgumentCaptor.forClass(ImFile.class);
        verify(imFileMapper).insert(imFileCaptor.capture());
        ImFile imFile = imFileCaptor.getValue();
        assertThat(imFile.getId()).isEqualTo(response.getFileId());
        assertThat(imFile.getBizType()).isEqualTo("avatar");
        assertThat(imFile.getFileName()).isEqualTo("avatar.PNG");
        assertThat(imFile.getObjectKey()).isEqualTo(objectNameCaptor.getValue());
        assertThat(imFile.getUrl()).isEqualTo(response.getUrl());
        assertThat(imFile.getExtension()).isEqualTo("png");
        assertThat(imFile.getSize()).isEqualTo(3L);
    }

    @Test
    void storeShouldRejectUnknownBizType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> fileStorageService.store(file, "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的业务类型");
    }

    @Test
    void storeShouldRejectExtensionNotInWhitelist() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> fileStorageService.store(file, "file"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void storeShouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.store(file, "image"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上传文件不能为空");
    }

    @Test
    void storeShouldRejectFileExceedingSizeLimit() {
        byte[] oversized = new byte[2 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", oversized);

        assertThatThrownBy(() -> fileStorageService.store(file, "image"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件大小超出限制");
    }

    @Test
    void storeShouldRejectFileWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noext", "image/png", new byte[]{1});

        assertThatThrownBy(() -> fileStorageService.store(file, "image"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件缺少扩展名");
    }

    @Test
    void fromCodeShouldParseCaseInsensitiveAndRejectInvalid() {
        assertThat(FileBizType.fromCode("AVATAR")).isEqualTo(FileBizType.AVATAR);
        assertThat(FileBizType.fromCode(" image ")).isEqualTo(FileBizType.IMAGE);
        assertThat(FileBizType.fromCode("other")).isNull();
        assertThat(FileBizType.fromCode("")).isNull();
        assertThat(FileBizType.fromCode(null)).isNull();
    }
}
