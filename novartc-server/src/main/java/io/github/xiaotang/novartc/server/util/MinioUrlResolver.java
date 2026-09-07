package io.github.xiaotang.novartc.server.util;

import io.github.xiaotang.novartc.server.config.MinioProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 将数据库中的 MinIO 对象 key 转换为当前环境的公共访问地址。
 */
@Component
@RequiredArgsConstructor
public class MinioUrlResolver {
    private final MinioProperties minioProperties;

    /**
     * 解析对象公共访问地址；兼容迁移期间遗留的完整 URL。
     */
    public String resolve(String objectKey) {
        if (StringUtils.isBlank(objectKey) || isAbsoluteUrl(objectKey)) {
            return objectKey;
        }
        String baseUrl = StringUtils.defaultIfBlank(
                minioProperties.getUrlPrefix(), minioProperties.getEndpoint());
        return StringUtils.removeEnd(baseUrl, "/") + "/"
                + minioProperties.getBucket() + "/" + StringUtils.removeStart(objectKey, "/");
    }

    /**
     * 从对象 key 或本应用生成的 MinIO 公共 URL 中还原对象 key。
     */
    public String resolveObjectKey(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (!isAbsoluteUrl(value)) {
            return StringUtils.removeStart(value, "/");
        }
        try {
            String path = URI.create(value).getPath();
            String bucketPath = "/" + minioProperties.getBucket() + "/";
            int bucketIndex = StringUtils.indexOf(path, bucketPath);
            if (bucketIndex < 0) {
                return null;
            }
            return StringUtils.substring(path, bucketIndex + bucketPath.length());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isAbsoluteUrl(String value) {
        return StringUtils.startsWithAny(StringUtils.lowerCase(value), "http://", "https://");
    }
}
