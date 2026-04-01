package io.github.xiaotang.novartc.server.config;

import java.util.LinkedHashSet;
import java.util.Set;

public class AdminContextHolder {
    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPER_ADMIN_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> MENU_CODES_HOLDER = new ThreadLocal<>();

    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    public static void setSuperAdmin(Boolean superAdmin) {
        SUPER_ADMIN_HOLDER.set(superAdmin);
    }

    public static boolean isSuperAdmin() {
        return Boolean.TRUE.equals(SUPER_ADMIN_HOLDER.get());
    }

    public static void setMenuCodes(Set<String> menuCodes) {
        MENU_CODES_HOLDER.set(menuCodes == null ? Set.of() : new LinkedHashSet<>(menuCodes));
    }

    public static Set<String> getMenuCodes() {
        Set<String> menuCodes = MENU_CODES_HOLDER.get();
        return menuCodes == null ? Set.of() : menuCodes;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        TOKEN_HOLDER.remove();
        SUPER_ADMIN_HOLDER.remove();
        MENU_CODES_HOLDER.remove();
    }
}
