package io.github.xiaotang.novartc.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MinIO 对象存储配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    /**
     * MinIO API 地址，如 http://localhost:6000
     */
    private String endpoint;
    /**
     * 访问密钥 ID
     */
    private String accessKey;
    /**
     * 访问密钥
     */
    private String secretKey;
    /**
     * 默认存储桶名称
     */
    private String bucket = "novartc";
    /**
     * 对外访问地址前缀，为空时使用 endpoint
     */
    private String urlPrefix;
    /**
     * 单文件大小上限（MB）
     */
    private Long maxFileSize = 20L;
    /**
     * 允许上传的扩展名白名单
     */
    private List<String> allowedExtensions;
}
