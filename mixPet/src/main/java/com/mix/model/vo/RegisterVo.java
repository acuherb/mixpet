package com.mix.model.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 注册时所传的模型
 */
public record RegisterVo(
        String username,
        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = ".{5,}", message = "密码至少5位")
        String password,
        @NotBlank(message = "姓名不能为空")
        String name,
        boolean sex,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String mail,
        @NotBlank(message = "验证码不能为空")
        String code,
        String birthday,
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String tel,
        String tel2,
        String address,
        String idCard
) {
    /**
     * 得到加密后的密码
     */
    public String getReallyPwd() {
        return com.mix.util.StrUtil.tranPwd(password);
    }
}