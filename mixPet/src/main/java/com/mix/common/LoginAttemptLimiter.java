package com.mix.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败限流：同一 IP + 账号连续失败 5 次锁定 15 分钟。
 * 单实例本地部署；重启后计数清零。
 */
@Component
public class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MS = 15 * 60 * 1000L;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private static final class Attempt {
        int failures;
        long blockedUntil;
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        Attempt a = attempts.get(key);
        return a != null && a.blockedUntil > System.currentTimeMillis();
    }

    public void recordFailure(String key) {
        if (key == null) return;
        long now = System.currentTimeMillis();
        attempts.compute(key, (k, a) -> {
            Attempt cur = a == null ? new Attempt() : a;
            if (cur.blockedUntil > now) return cur;
            cur.failures++;
            if (cur.failures >= MAX_FAILURES) cur.blockedUntil = now + LOCK_MS;
            return cur;
        });
        if (attempts.size() > 2000) {
            attempts.entrySet().removeIf(e -> e.getValue().blockedUntil < now
                    && e.getValue().failures < MAX_FAILURES);
        }
    }

    public void clear(String key) {
        if (key != null) attempts.remove(key);
    }
}
