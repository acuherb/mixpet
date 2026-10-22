package com.mix.common;


/**
 * 通用返回结果，封装增删改的响应数据
 *
 * @param code 1成功 0失败
 * @param msg  提示信息（可空）
 * @param data 业务数据（可空，如验证 token；序列化为 JSON 的 data 字段）
 */
public record Result(Integer code, String msg, Object data) {
    public static Result choice(String msg, boolean res) {
        if (res) return new Result(1, msg + "成功", null);
        else return new Result(0, msg + "失败", null);
    }

    public static Result error(String msg) {
        return new Result(0, msg, null);
    }

    public static Result success(String msg) {
        return new Result(1, msg, null);
    }

    /** 带业务数据的成功响应（如验证码 token 这类需要回传前端的值） */
    public static Result data(Object data) {
        return new Result(1, null, data);
    }
}
