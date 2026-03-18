package io.github.xiaotang.novartc.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created on 2026/3/18 16:00
 *
 * @author tangj
 * @description
 */
public class MD5Util {

    private MD5Util() {
        // 工具类不可实例化
    }

    /**
     * 默认字符集 UTF-8
     */
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * MD5 算法名称
     */
    private static final String ALGORITHM_MD5 = "MD5";

    /**
     * 将字符串进行 MD5 加密（默认 UTF-8 编码）
     *
     * @param text 明文字符串
     * @return 32 位小写 MD5 字符串，若加密失败返回 null
     */
    public static String md5(String text) {
        return md5(text, DEFAULT_CHARSET);
    }

    /**
     * 将字符串按照指定字符集进行 MD5 加密
     *
     * @param text    明文字符串
     * @param charset 字符集名称，例如 "UTF-8", "GBK"
     * @return 32 位小写 MD5 字符串，若加密失败返回 null
     */
    public static String md5(String text, String charset) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = text.getBytes(charset);
            return md5(bytes);
        } catch (Exception e) {
            // 记录日志或抛出运行时异常
            throw new RuntimeException("MD5 encryption failed", e);
        }
    }

    /**
     * 将字节数组进行 MD5 加密
     *
     * @param bytes 字节数组
     * @return 32 位小写 MD5 字符串，若加密失败返回 null
     */
    public static String md5(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM_MD5);
            byte[] digest = md.digest(bytes);
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * 计算文件的 MD5 值
     *
     * @param file 文件对象
     * @return 32 位小写 MD5 字符串，若文件不存在或读取失败返回 null
     */
    public static String md5(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM_MD5);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            byte[] digest = md.digest();
            return bytesToHex(digest);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate file MD5", e);
        }
    }

    /**
     * 加盐 MD5 加密（默认 UTF-8 编码）
     *
     * @param text 明文字符串
     * @param salt 盐值
     * @return 加盐后的 32 位小写 MD5 字符串
     */
    public static String md5WithSalt(String text, String salt) {
        if (text == null) {
            return null;
        }
        String combined = text + (salt == null ? "" : salt);
        return md5(combined);
    }

    /**
     * 验证明文字符串是否与给定的 MD5 值匹配（默认 UTF-8 编码）
     *
     * @param text      明文字符串
     * @param md5String 待验证的 MD5 字符串（32 位小写）
     * @return 匹配返回 true，否则 false
     */
    public static boolean verify(String text, String md5String) {
        return verify(text, md5String, DEFAULT_CHARSET);
    }

    /**
     * 验证明文字符串（指定字符集）是否与给定的 MD5 值匹配
     *
     * @param text      明文字符串
     * @param md5String 待验证的 MD5 字符串（32 位小写）
     * @param charset   字符集名称
     * @return 匹配返回 true，否则 false
     */
    public static boolean verify(String text, String md5String, String charset) {
        if (text == null || md5String == null || md5String.length() != 32) {
            return false;
        }
        String computed = md5(text, charset);
        return computed != null && computed.equalsIgnoreCase(md5String);
    }

    /**
     * 验证加盐后的明文字符串是否与给定的 MD5 值匹配（默认 UTF-8 编码）
     *
     * @param text      明文字符串
     * @param salt      盐值
     * @param md5String 待验证的加盐 MD5 字符串（32 位小写）
     * @return 匹配返回 true，否则 false
     */
    public static boolean verifyWithSalt(String text, String salt, String md5String) {
        String computed = md5WithSalt(text, salt);
        return computed != null && computed.equalsIgnoreCase(md5String);
    }

    /**
     * 将字节数组转换为小写十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // 将每个字节转换为两位十六进制数
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}