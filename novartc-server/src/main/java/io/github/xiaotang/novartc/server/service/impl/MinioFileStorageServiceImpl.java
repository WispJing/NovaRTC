package io.github.xiaotang.novartc.server.service.impl;

import io.github.xiaotang.novartc.common.enums.FileBizType;
import io.github.xiaotang.novartc.common.exception.BusinessException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.github.xiaotang.novartc.server.config.MinioProperties;
import io.github.xiaotang.novartc.server.config.UserContextHolder;
import io.github.xiaotang.novartc.server.dto.FileUploadResponse;
import io.github.xiaotang.novartc.server.entity.mysql.ImFile;
import io.github.xiaotang.novartc.server.mapper.ImFileMapper;
import io.github.xiaotang.novartc.server.service.FileStorageService;
import io.github.xiaotang.novartc.server.util.MinioUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 基于 MinIO 的文件存储实现。
 */
@Service
@RequiredArgsConstructor
public class MinioFileStorageServiceImpl implements FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(MinioFileStorageServiceImpl.class);

    private static final long BYTES_PER_MB = 1024 * 1024L;

    private final MinioProperties minioProperties;
    private final MinioUtil minioUtil;
    private final ImFileMapper imFileMapper;

    /**
     * 启动时确保存储桶存在并开启公共读；失败仅记录日志，不阻塞应用启动，
     * MinIO 不可用时上传接口会自然快速失败
     */
    @PostConstruct
    public void initBucket() {
        String bucket = minioProperties.getBucket();
        try {
            if (minioUtil.makeBucket(bucket)) {
                logger.info("MinIO 存储桶初始化完成: {}", bucket);
            }
            minioUtil.setBucketPublicRead(bucket);
        } catch (RuntimeException e) {
            logger.error("MinIO 存储桶初始化失败，文件上传功能暂不可用: {}", e.getMessage(), e);
        }
    }

    @Override
    public FileUploadResponse store(MultipartFile file, String bizType) {
        FileBizType type = FileBizType.fromCode(bizType);
        if (type == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "不支持的业务类型: " + bizType);
        }
        validate(file);
        String extension = resolveExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        // 按业务类型分目录，日期目录使用 UTC，文件名不含用户输入，避免路径穿越
        String objectName = type.code() + "/"
                + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/" + fileName;

        String bucket = minioProperties.getBucket();
        long partSize = minioProperties.getMaxFileSize() * BYTES_PER_MB;
        try (InputStream inputStream = file.getInputStream()) {
            minioUtil.putObject(bucket, objectName, inputStream, file.getContentType(), partSize);
        } catch (IOException e) {
            logger.error("读取上传文件流失败: {}", file.getOriginalFilename(), e);
            throw new BusinessException(ResultCode.SERVER_ERROR.code(), "文件上传失败，请重试");
        }
        logger.info("文件上传成功: 用户={}, 业务类型={}, 原始文件={}, 对象={}/{}",
                UserContextHolder.getUserId(), type.code(), file.getOriginalFilename(), bucket, objectName);

        String url = buildUrl(bucket, objectName);
        String fileId = registerFile(type, file, extension, objectName, url);

        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(fileId);
        response.setBizType(type.code());
        response.setFileName(fileName);
        response.setOriginalName(file.getOriginalFilename());
        response.setUrl(url);
        response.setSize(file.getSize());
        response.setContentType(file.getContentType());
        return response;
    }

    /**
     * 登记文件信息到 im_file 表，返回记录ID
     */
    private String registerFile(FileBizType type, MultipartFile file, String extension,
                                String objectName, String url) {
        ImFile imFile = new ImFile();
        imFile.setId(UUID.randomUUID().toString().replace("-", ""));
        imFile.setBizType(type.code());
        imFile.setFileName(file.getOriginalFilename());
        imFile.setObjectKey(objectName);
        imFile.setUrl(url);
        imFile.setSize(file.getSize());
        imFile.setContentType(file.getContentType());
        imFile.setExtension(extension);
        imFile.setUploaderId(UserContextHolder.getUserId());
        imFileMapper.insert(imFile);
        return imFile.getId();
    }

    /**
     * 校验文件非空、类型与大小
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "上传文件不能为空");
        }
        resolveExtension(file.getOriginalFilename());
        long maxBytes = minioProperties.getMaxFileSize() * BYTES_PER_MB;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(),
                    "文件大小超出限制: 最大 " + minioProperties.getMaxFileSize() + "MB");
        }
    }

    /**
     * 提取并校验扩展名，不在白名单内抛出参数异常
     */
    private String resolveExtension(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "文件名不能为空");
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "文件缺少扩展名");
        }
        String extension = originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        List<String> allowedExtensions = minioProperties.getAllowedExtensions();
        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.code(), "不支持的文件类型: " + extension);
        }
        return extension;
    }

    /**
     * 拼接对外访问地址，优先使用配置的 urlPrefix
     */
    private String buildUrl(String bucket, String objectName) {
        String baseUrl = StringUtils.isNotBlank(minioProperties.getUrlPrefix())
                ? minioProperties.getUrlPrefix()
                : minioProperties.getEndpoint();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + bucket + "/" + objectName;
    }
}
