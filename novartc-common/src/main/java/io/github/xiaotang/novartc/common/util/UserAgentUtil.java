package io.github.xiaotang.novartc.common.util;

/**
 * Created on 2026/3/19 14:05
 *
 * @author tangj
 * @description
 */
public class UserAgentUtil {
    public static String parseClientTypeFromUserAgent(String userAgent) {
        if (userAgent == null) {
            return "UNKNOWN";
        }
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "APP";
        } else if (userAgent.contains("ipad")) {
            return "PAD";
        } else {
            return "WEB";
        }
    }
}
