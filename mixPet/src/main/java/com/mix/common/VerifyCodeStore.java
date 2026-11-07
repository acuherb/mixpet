package com.mix.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码服务端存储。
 * 验证码只保存在服务端内存，不下发到 JWT / 接口响应（JWT 里只放一次性 id）。
 * 单实例本地部署；进程重启后未使用的验证码自然失效。
 */
@Component
public class VerifyCodeStore {
    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final long SEND_INTERVAL_MS = 30 * 1000L;
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();

    private static final class Entry {
        final String code;
        final String mail;
        final String username;
        final long expireAt;
        int attempts;

        Entry(String code, String mail, String username, long expireAt) {
            this.code = code;
            this.mail = mail;
            this.username = username;
            this.expireAt = expireAt;
        }
    }

    /** 发送节流：同一邮箱 30 秒内只允许发一次 */
    public boolean trySend(String mail) {
        if (mail == null) return false;
        long now = System.currentTimeMillis();
        Long last = lastSent.putIfAbsent(mail, now);
        if (last != null && now - last < SEND_INTERVAL_MS) return false;
        lastSent.put(mail, now);
        if (lastSent.size() > 1000) {
            lastSent.entrySet().removeIf(e -> now - e.getValue() > SEND_INTERVAL_MS * 2);
        }
        return true;
    }

    /** 登记验证码，返回一次性 id（写进 JWT 的 jti，不含验证码本身） */
    public String issue(String mail, String username, String code) {
        long now = System.currentTimeMillis();
        codes.entrySet().removeIf(e -> e.getValue().expireAt < now);
        String id = UUID.randomUUID().toString();
        codes.put(id, new Entry(code, mail, username, now + TTL_MS));
        return id;
    }

    /**
     * 校验并消费验证码：成功即删除（一次性），失败累计次数，超过上限作废。
     * computeIfPresent 保证并发下同一验证码只会被消费一次。
     */
    public boolean verify(String id, String mail, String username, String input) {
        if (id == null || mail == null || input == null) return false;
        long now = System.currentTimeMillis();
        final boolean[] ok = {false};
        codes.computeIfPresent(id, (key, e) -> {
            if (e.expireAt < now) return null;
            if (!e.mail.equalsIgnoreCase(mail)) return e;
            if (e.username != null && !e.username.equals(username)) return e;
            if (e.attempts >= MAX_ATTEMPTS) return null;
            if (e.code.equalsIgnoreCase(input.trim())) {
                ok[0] = true;
                return null;
            }
            e.attempts++;
            return e.attempts >= MAX_ATTEMPTS ? null : e;
        });
        return ok[0];
    }
}
