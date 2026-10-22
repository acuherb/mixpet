package com.mix.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码时传入的模型
 */
public record PasswordVo(
        @NotBlank(message = "原密码不能为空")
        String oldPwd,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "新密码至少6位")
        String newPwd
) {
}