package io.github.xiaotang.novartc.server.util;

import io.github.xiaotang.novartc.common.exception.ApplicationException;
import io.github.xiaotang.novartc.common.result.ResultCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * MinIO 对象存储工具类，封装 MinioClient 常用操作。
 */
@Component
@RequiredArgsConstructor
public class MinioUtil {
    private static final Logger logger = LoggerFactory.getLogger(MinioUtil.class);

    /**
     * 桶公共读策略模板，允许匿名下载桶内对象
     */
    private static final String PUBLIC_READ_POLICY_TEMPLATE = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": {"AWS": ["*"]},
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """;

    private final MinioClient minioClient;

    /**
     * 检查存储桶是否存在。
     */
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw wrap("检查存储桶失败", bucketName, e);
        }
    }

    /**
     * 创建存储桶，已存在则跳过。
     *
     * @return 是否本次新建
     */
    public boolean makeBucket(String bucketName) {
        if (bucketExists(bucketName)) {
            return false;
        }
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("MinIO 存储桶已创建: {}", bucketName);
            return true;
        } catch (Exception e) {
            throw wrap("创建存储桶失败", bucketName, e);
        }
    }

    /**
     * 设置存储桶公共读策略，允许匿名下载。
     */
    public void setBucketPublicRead(String bucketName) {
        String policy = PUBLIC_READ_POLICY_TEMPLATE.formatted(bucketName);
        try {
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
            logger.info("MinIO 存储桶已开启公共读: {}", bucketName);
        } catch (Exception e) {
            throw wrap("设置存储桶公共读策略失败", bucketName, e);
        }
    }

    /**
     * 流式上传对象。
     *
     * @param partSize 分片大小（字节），未知总长度时必须大于 5MiB
     */
    public void putObject(String bucketName, String objectName, InputStream inputStream,
                          String contentType, long partSize) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, -1, partSize)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            throw wrap("上传对象失败", bucketName + "/" + objectName, e);
        }
    }

    /**
     * 以流的形式获取对象，调用方负责关闭流。
     */
    public InputStream getObject(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw wrap("下载对象失败", bucketName + "/" + objectName, e);
        }
    }

    /**
     * 获取对象元数据。
     */
    public StatObjectResponse statObject(String bucketName, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw wrap("获取对象元数据失败", bucketName + "/" + objectName, e);
        }
    }

    /**
     * 删除对象。
     */
    public void removeObject(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw wrap("删除对象失败", bucketName + "/" + objectName, e);
        }
    }

    /**
     * MinIO SDK 声明的受检异常类型较多，统一包装为应用异常并记录日志
     */
    private ApplicationException wrap(String action, String target, Exception e) {
        logger.error("{}: {}, 原因: {}", action, target, e.getMessage(), e);
        return new ApplicationException(action + ": " + target, e, ResultCode.SERVER_ERROR.code());
    }
}
