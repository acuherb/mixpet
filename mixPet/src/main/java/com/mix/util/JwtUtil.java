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

    public String generateVerifyCode(String mail, String code) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "verify_code");
        claims.put("mail", mail);
        claims.put("code", code);
        Date now = new Date();
        Date exp = new Date(now.getTime() + VERIFY_CODE_EXPIRE_MINUTES * 60 * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getKey())
                .compact();
    }

    public String validateAndGetCode(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!"verify_code".equals(claims.get("type"))) return null;
            if (claims.getExpiration().before(new Date())) return null;
            return claims.get("code", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String getMailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("mail", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}