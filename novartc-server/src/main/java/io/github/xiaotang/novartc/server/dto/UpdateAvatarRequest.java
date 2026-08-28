package io.github.xiaotang.novartc.server.dto;

import lombok.Data;

/**
 * 更新头像请求，携带文件上传返回的文件记录ID。
 */
@Data
public class UpdateAvatarRequest {
    /**
     * im_file 记录ID（bizType 必须为 avatar，且为本人上传）
     */
    private String fileId;
}
