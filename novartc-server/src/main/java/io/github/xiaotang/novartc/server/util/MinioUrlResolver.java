package io.github.xiaotang.novartc.server.util;

import io.github.xiaotang.novartc.server.config.MinioProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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

    private boolean isAbsoluteUrl(String value) {
        return StringUtils.startsWithAny(StringUtils.lowerCase(value), "http://", "https://");
    }
}
