package com.mix.security.login;

import com.mix.common.Result;
import com.mix.service.IAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * 登录成功/失败处理器：统一失败文案防账号枚举，并把登录行为写入审计日志。
 * 不打印 principal（含密码哈希、角色对象等敏感信息）。
 */
public class LoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private final IAuditLogService auditLogService;

    public LoginHandler(IAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse resp, Authentication authentication) {
        req.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        audit("login", "登录成功", req);
        JsonUtil.write(Result.success("登录成功"), resp);
    }

    /** 账号是否存在/密码是否错误统一文案；审核状态提示保留（产品要求） */
    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse resp, AuthenticationException ae) {
        String msg;
        if (ae instanceof org.springframework.security.authentication.LockedException) {
            msg = ae.getMessage();
        } else {
            String raw = ae.getMessage();
            msg = (ae instanceof UsernameNotFoundException && raw != null && raw.contains("审核"))
                    ? raw : "账号或密码错误";
        }
        audit("loginFail", "登录失败：" + msg, req);
        JsonUtil.write(Result.error(msg), resp);
    }

    /** 审计失败不能影响登录本身；只记账号名与结果，绝不记录密码 */
    private void audit(String action, String detail, HttpServletRequest req) {
        try {
            Object user = req.getAttribute("loginUsername");
            auditLogService.log("auth", action, "账号登录",
                    (user == null ? "" : user + " ") + detail, req.getRemoteAddr());
        } catch (Exception ignore) {
            // 审计表缺失或写入异常时不影响登录
        }
    }
}
