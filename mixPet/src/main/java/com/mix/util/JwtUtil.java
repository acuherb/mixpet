package com.mix.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final long VERIFY_CODE_EXPIRE_MINUTES = 5;

    @Value("${app.verify-code-secret:}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成验证码 Token
     * @param mail     用于绑定验证码与邮箱，防止跨邮箱使用
     * @param username 可选，用于找回密码场景绑定账号
     * @param code     验证码
     */
    public String generateVerifyCode(String mail, String username, String code) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "verify_code");
        claims.put("mail", mail);
        claims.put("code", code);
        if (username != null) {
            claims.put("username", username);
        }
        Date now = new Date();
        Date exp = new Date(now.getTime() + VERIFY_CODE_EXPIRE_MINUTES * 60 * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getKey())
                .compact();
    }

    /**
     * 验证 Token 并提取验证码（用于注册：只校验 mail）
     * @param token 待验证的 JWT
     * @param expectedMail 期望的邮箱（前端传回，用于绑定校验）
     */
    public String validateAndGetCode(String token, String expectedMail) {
        try {
            Claims claims = parseClaims(token);
            if (!"verify_code".equals(claims.get("type"))) return null;
            if (claims.getExpiration().before(new Date())) return null;
            if (!expectedMail.equals(claims.get("mail"))) return null;
            return claims.get("code", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证 Token 并提取验证码（用于找回密码：校验 mail + username）
     */
    public String validateAndGetCode(String token, String expectedMail, String expectedUsername) {
        try {
            Claims claims = parseClaims(token);
            if (!"verify_code".equals(claims.get("type"))) return null;
            if (claims.getExpiration().before(new Date())) return null;
            if (!expectedMail.equals(claims.get("mail"))) return null;
            if (!expectedUsername.equals(claims.get("username"))) return null;
            return claims.get("code", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}