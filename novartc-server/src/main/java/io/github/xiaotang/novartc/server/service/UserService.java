package io.github.xiaotang.novartc.server.service;

/**
 * 用户信息服务。
 */
public interface UserService {

    /**
     * 更新当前登录用户头像。
     *
     * @param fileId 文件上传返回的 im_file 记录ID，必须为本人上传的 avatar 类型文件
     * @return 新的头像访问地址
     */
    String updateAvatar(String fileId);
}
