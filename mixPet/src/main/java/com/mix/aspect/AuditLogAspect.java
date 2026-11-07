package com.mix.aspect;

import com.mix.annotation.AuditLog;
import com.mix.common.Result;
import com.mix.service.IAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 审计日志切面：拦截 @AuditLog 方法，成功/失败都留痕，审计异常记录告警（不静默吞）。
 */
@Aspect
@Component
public class AuditLogAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private final IAuditLogService auditLogService;

    public AuditLogAspect(IAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        Object result;
        try {
            result = point.proceed();
        } catch (Throwable t) {
            safeLog(auditLog, point, "失败(异常:" + t.getClass().getSimpleName() + ")");
            throw t;
        }
        // Result.error 也是正常返回，必须区分业务失败，避免把被拒绝的操作记成"做成了"
        String status = "成功";
        if (result instanceof Result r && r.code() != null && r.code() == 0) {
            status = "失败(业务:" + (r.msg() == null ? "" : r.msg()) + ")";
        }
        safeLog(auditLog, point, status);
        return result;
    }

    private void safeLog(AuditLog auditLog, ProceedingJoinPoint point, String status) {
        try {
            String target = auditLog.target().isEmpty() ? describeArgs(point) : auditLog.target();
            String detail = "[" + status + "] 方法 " + point.getSignature().getName()
                    + " 参数 " + describeArgs(point);
            auditLogService.log(auditLog.module(), auditLog.action(), target, detail, ip());
        } catch (Exception e) {
            log.warn("审计日志写入失败（不影响主业务）", e);
        }
    }

    /** 参数摘要：敏感对象只记类名，避免密码/证件号/客户资料落库 */
    private String describeArgs(ProceedingJoinPoint point) {
        Object[] args = point.getArgs();
        if (args == null || args.length == 0) return "";
        return Arrays.stream(args)
                .filter(java.util.Objects::nonNull)
                .map(a -> {
                    String cls = a.getClass().getSimpleName();
                    if (cls.contains("Password") || cls.contains("Login") || cls.contains("Register")
                            || cls.contains("Client") || cls.contains("Doctor")) {
                        return cls + "(敏感参数已省略)";
                    }
                    String s = a.toString();
                    return s.length() > 60 ? s.substring(0, 60) + "…" : s;
                })
                .collect(Collectors.joining("; "));
    }

    /** 只取直连地址：X-Forwarded-For 可由客户端伪造，未配置可信代理前不采信 */
    private String ip() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
