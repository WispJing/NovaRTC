package io.github.xiaotang.novartc.server.entity.mysql;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.xiaotang.novartc.server.entity.mysql.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 * 文件信息表
 * </p>
 *
 * @author xiaotang
 * @since 2026-08-27
 */
@Getter
@Setter
@ToString
@TableName("im_file")
public class ImFile extends BaseEntity {

    /**
     * 业务类型：avatar/image/video/audio/file
     */
    @TableField("biz_type")
    private String bizType;

    /**
     * 原始文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 对象存储key
     */
    @TableField("object_key")
    private String objectKey;

    /**
     * 文件访问URL
     */
    @TableField("url")
    private String url;

    /**
     * 文件大小（字节）
     */
    @TableField("size")
    private Long size;

    /**
     * MIME类型
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 扩展名
     */
    @TableField("extension")
    private String extension;

    /**
     * 上传人用户ID
     */
    @TableField("uploader_id")
    private String uploaderId;
}
