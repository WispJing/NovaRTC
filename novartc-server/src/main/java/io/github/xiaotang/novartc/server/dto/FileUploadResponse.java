package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * 文件上传结果。
 */
@Data
public class FileUploadResponse {
    /**
     * 存储文件名（不含日期目录）
     */
    private String fileName;
    /**
     * 原始文件名
     */
    private String originalName;
    /**
     * 可公开访问的文件地址
     */
    private String url;
    /**
     * 文件大小（字节）
     */
    private Long size;
    /**
     * 文件 MIME 类型
     */
    private String contentType;
}
