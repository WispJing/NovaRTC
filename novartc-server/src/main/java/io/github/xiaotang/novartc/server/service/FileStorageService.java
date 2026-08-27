package io.github.xiaotang.novartc.server.service;

import io.github.xiaotang.novartc.server.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务。
 */
public interface FileStorageService {

    /**
     * 上传文件到对象存储，返回可公开访问的文件地址。
     *
     * @param file 上传的文件
     * @return 上传结果
     */
    FileUploadResponse store(MultipartFile file);
}
