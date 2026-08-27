package io.github.xiaotang.novartc.server.controller;

import io.github.xiaotang.novartc.server.dto.FileUploadResponse;
import io.github.xiaotang.novartc.server.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器，接口位于 /api/** 下，需登录后访问。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/file")
public class FileController {
    private final FileStorageService fileStorageService;

    /**
     * 文件上传，返回可公开访问的文件地址。
     */
    @PostMapping("/upload")
    public FileUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return fileStorageService.store(file);
    }
}
