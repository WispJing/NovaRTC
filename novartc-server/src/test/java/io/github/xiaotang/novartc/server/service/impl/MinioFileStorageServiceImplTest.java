package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.dto.FileUploadResponse;
import io.github.xiaotang.novartc.server.util.MinioUtil;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * MinIO 文件存储服务单元测试，不启动 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceImplTest {

    @Mock
    private MinioUtil minioUtil;

    private MinioProperties minioProperties;

    private MinioFileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        minioProperties = new MinioProperties();
        minioProperties.setEndpoint("http://localhost:6000");
        minioProperties.setBucket("novartc");
        minioProperties.setMaxFileSize(1L);
        minioProperties.setAllowedExtensions(List.of("jpg", "png", "pdf"));
        fileStorageService = new MinioFileStorageServiceImpl(minioProperties, minioUtil);
    }

    @Test
    void storeShouldUploadAndReturnPublicUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.PNG", "image/png", new byte[]{1, 2, 3});

        FileUploadResponse response = fileStorageService.store(file);

        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(minioUtil).putObject(eq("novartc"), objectNameCaptor.capture(),
                any(InputStream.class), eq("image/png"), anyLong());

        String dateDir = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(objectNameCaptor.getValue()).startsWith(dateDir + "/");
        assertThat(objectNameCaptor.getValue()).endsWith(".png");

        assertThat(response.getOriginalName()).isEqualTo("avatar.PNG");
        assertThat(response.getSize()).isEqualTo(3L);
        assertThat(response.getContentType()).isEqualTo("image/png");
        assertThat(response.getUrl())
                .isEqualTo("http://localhost:6000/novartc/" + objectNameCaptor.getValue());
    }

    @Test
    void storeShouldRejectExtensionNotInWhitelist() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> fileStorageService.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    void storeShouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上传文件不能为空");
    }

    @Test
    void storeShouldRejectFileExceedingSizeLimit() {
        byte[] oversized = new byte[2 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", oversized);

        assertThatThrownBy(() -> fileStorageService.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件大小超出限制");
    }

    @Test
    void storeShouldRejectFileWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noext", "image/png", new byte[]{1});

        assertThatThrownBy(() -> fileStorageService.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件缺少扩展名");
    }
}
