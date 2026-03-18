package io.github.xiaotang.novartc.common.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Created on 2026/3/18 14:53
 *
 * @author tangj
 * @description
 */
public class JwtUtil {
    // 建议 >= 32 字节
    private static final String SECRET = "NovaRTC-Secret-Key-xiaotang-123456";

    // 过期时间（7天）
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    /**
     * 生成 Token
     */
    public static String generateToken(String userId, Map<String, Object> claims) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + EXPIRATION))
                .claims(claims)
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析 Token
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 userId
     */
    public static String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 是否过期
     */
    public static boolean isExpired(String token) {
        return parseToken(token)
                .getExpiration()
                .before(new Date());
    }

    /**
     * 校验 Token
     */
    public static boolean validate(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刷新 Token
     */
    public static String refreshToken(String token) {
        Claims claims = parseToken(token);
        return generateToken(claims.getSubject(), claims);
    }
}
