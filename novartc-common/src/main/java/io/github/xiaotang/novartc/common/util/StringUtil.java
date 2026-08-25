package io.github.xiaotang.novartc.common.util;

import java.util.UUID;

/**
 * 字符串通用工具类。
 *
 * <p>收拢各业务模块中重复实现的私有工具方法（如 32 位无横线 UUID 生成），
 * 并补充少量空值兜底方法。判空类能力请直接使用项目已依赖的
 * {@code org.apache.commons.lang3.StringUtils}，本类不做重复实现。</p>
 */
public class StringUtil {

    private StringUtil() {
        // 工具类不可实例化
    }

    /**
     * 生成 32 位无横线 UUID。
     *
     * <p>IM 用户、会话、好友请求等主键统一使用无横线 UUID，
     * 避免各业务类重复编写 {@code UUID.randomUUID().toString().replace("-", "")}。</p>
     *
     * @return 32 位小写十六进制字符串
     */
    public static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 将 null 转换为空字符串，非 null 原样返回。
     *
     * <p>用于组装推送内容、返回 VO 等场景的空值兜底，
     * 避免调用方处理 null 或拼接时产生 "null" 字面量。</p>
     *
     * @param str 原始字符串
     * @return str 为 null 时返回 ""，否则返回 str 本身
     */
    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    /**
     * 去除首尾空白，并将 null 转换为空字符串。
     *
     * <p>用于接收外部输入（表单、消息内容等）后的归一化，
     * 一次调用同时完成 null 兜底与 trim。</p>
     *
     * @param str 原始字符串
     * @return str 为 null 时返回 ""，否则返回 str.trim()
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * 字符串为空白（null、空串或全空白字符）时返回默认值。
     *
     * <p>用于业务参数缺省值填充，例如客户端类型缺省时回退到默认值，
     * 比单纯的 null 判断更贴近业务语义。</p>
     *
     * @param str        原始字符串
     * @param defaultStr 空白时返回的默认值
     * @return str 为空白时返回 defaultStr，否则返回 str 本身
     */
    public static String defaultIfBlank(String str, String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    /**
     * 判断字符串是否为空白（仅供本类内部使用，外部判空请使用 commons-lang3）。
     *
     * @param str 待判断字符串
     * @return null、空串或全为空白字符时返回 true
     */
    private static boolean isBlank(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
