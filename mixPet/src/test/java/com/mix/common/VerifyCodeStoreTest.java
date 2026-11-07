package com.mix.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证码：一次性、邮箱绑定、尝试次数上限、发送节流 */
class VerifyCodeStoreTest {

    @Test
    void codeIsOneTimeAndCannotBeReused() {
        VerifyCodeStore store = new VerifyCodeStore();
        String id = store.issue("a@test.com", null, "123456");
        assertTrue(store.verify(id, "a@test.com", null, "123456"));
        assertFalse(store.verify(id, "a@test.com", null, "123456"));
    }

    @Test
    void codeIsBoundToMailAndUsername() {
        VerifyCodeStore store = new VerifyCodeStore();
        String id = store.issue("a@test.com", "alice", "123456");
        assertFalse(store.verify(id, "b@test.com", "alice", "123456"));
        assertFalse(store.verify(id, "a@test.com", "bob", "123456"));
        assertTrue(store.verify(id, "a@test.com", "alice", "123456"));
    }

    @Test
    void tooManyWrongAttemptsInvalidateCode() {
        VerifyCodeStore store = new VerifyCodeStore();
        String id = store.issue("a@test.com", null, "123456");
        for (int i = 0; i < 5; i++) assertFalse(store.verify(id, "a@test.com", null, "000000"));
        assertFalse(store.verify(id, "a@test.com", null, "123456"));
    }

    @Test
    void unknownIdIsRejected() {
        VerifyCodeStore store = new VerifyCodeStore();
        assertFalse(store.verify("not-issued", "a@test.com", null, "123456"));
    }

    @Test
    void sameMailCannotSendTwiceWithin30Seconds() {
        VerifyCodeStore store = new VerifyCodeStore();
        assertTrue(store.trySend("a@test.com"));
        assertFalse(store.trySend("a@test.com"));
        assertTrue(store.trySend("b@test.com"));
    }
}
