package com.ship.util;

import org.springframework.lang.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用于判空、加密等字符串操作
 */
public final class StrUtil {
    /** BCrypt 编码器单例（线程安全，避免每次 new 的重复初始化开销） */
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /**
     * 判断字段是否为空
     *
     * @param str 前端传来的字段
     * @return 不为空
     */
    public static boolean isWhite(@Nullable String str) {
        return str == null || str.isBlank();
    }

    /**
     * 给明文密码加盐加密
     *
     * @param pwd 明文密码
     * @return 加盐加密之后的密码
     */
    /**
     * 校验明文密码是否匹配加密密码
     */
    public static boolean matches(String rawPwd, String encodedPwd) {
        if (rawPwd == null || encodedPwd == null) return false;
        return ENCODER.matches(rawPwd, encodedPwd);
    }

    public static String tranPwd(String pwd) {
        return ENCODER.encode(pwd);
    }
}
