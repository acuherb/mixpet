package com.mix.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final long VERIFY_CODE_EXPIRE_MINUTES = 5;

    @Value("${app.verify-code-secret:}")
    private String secret;

    /** 启动时强校验：密钥缺失、过短或仍是仓库公开模板值 → 拒绝启动，不再用确定性密钥兜底 */
    @PostConstruct
    public void checkSecret() {
        if (secret == null || secret.length() < 32
                || secret.equals("YourSecureJwtSecretKeyAtLeast32Chars!")) {
            throw new IllegalStateException(
                    "VERIFY_CODE_SECRET 未配置、短于 32 字符或仍是公开模板值，拒绝启动（请在 .env 中配置强随机密钥）");
        }
    }

    /**
     * 签名密钥：JJWT 要求 HMAC-SHA 密钥 ≥256bit(32字节)。
     * checkSecret 已保证配置不为空；此处仍兼容 Base64 形式与超长密钥。
     */
    private SecretKey getKey() {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(secret);
        } catch (Exception notBase64) {
            raw = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (raw.length < 32) {
            try {
                raw = MessageDigest.getInstance("SHA-256").digest(raw);
            } catch (Exception never) {
                raw = "mixpet-verify-code-fallback-key".getBytes(StandardCharsets.UTF_8);
            }
        } else if (raw.length > 64) {
            raw = java.util.Arrays.copyOf(raw, 64);
        }
        return Keys.hmacShaKeyFor(raw);
    }

    /**
     * 生成验证码令牌：只携带一次性 id（jti），验证码本身只存服务端 VerifyCodeStore。
     * @param mail     绑定邮箱，防止跨邮箱使用
     * @param username 可选，用于找回密码场景绑定账号
     * @param jti      VerifyCodeStore.issue 返回的一次性 id
     */
    public String generateVerifyCode(String mail, String username, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "verify_code");
        claims.put("mail", mail);
        claims.put("jti", jti);
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

    /** 校验令牌并返回服务端存储 id（注册场景，只绑定邮箱） */
    public String validateAndGetJti(String token, String expectedMail) {
        return validateAndGetJti(token, expectedMail, null);
    }

    /** 校验令牌并返回服务端存储 id（找回密码场景，绑定邮箱 + 账号） */
    public String validateAndGetJti(String token, String expectedMail, String expectedUsername) {
        try {
            Claims claims = parseClaims(token);
            if (!"verify_code".equals(claims.get("type"))) return null;
            if (claims.getExpiration().before(new Date())) return null;
            if (expectedMail == null || !expectedMail.equalsIgnoreCase(claims.get("mail", String.class)))
                return null;
            String boundUser = claims.get("username", String.class);
            if (boundUser != null && !boundUser.equals(expectedUsername)) return null;
            String jti = claims.get("jti", String.class);
            return jti == null || jti.isBlank() ? null : jti;
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