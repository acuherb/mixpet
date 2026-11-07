package com.mix.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 登录失败锁定：连续失败 5 次锁定、成功后清零 */
class LoginAttemptLimiterTest {

    @Test
    void fiveFailuresBlockTheAccount() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        String key = "127.0.0.1|alice";
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
            assertFalse(limiter.isBlocked(key));
        }
        limiter.recordFailure(key);
        assertTrue(limiter.isBlocked(key));
    }

    @Test
    void successfulLoginClearsFailures() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        String key = "127.0.0.1|bob";
        limiter.recordFailure(key);
        limiter.recordFailure(key);
        limiter.clear(key);
        assertFalse(limiter.isBlocked(key));
        for (int i = 0; i < 4; i++) limiter.recordFailure(key);
        assertFalse(limiter.isBlocked(key), "清零后应重新从 0 计数");
    }
}
