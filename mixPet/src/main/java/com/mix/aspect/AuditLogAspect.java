package com.mix.aspect;

import com.mix.annotation.AuditLog;
import com.mix.service.IAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 审计日志切面：拦截 @AuditLog 方法，成功执行后记日志
 * （失败不记——业务自己会返回错误提示；只记"做成了"的高危操作）
 */
@Aspect
@Component
public class AuditLogAspect {
    private final IAuditLogService auditLogService;

    public AuditLogAspect(IAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint point, AuditLog auditLog) throws Throwable {
        Object result = point.proceed();
        try {
            String target = auditLog.target().isEmpty() ? describeArgs(point) : auditLog.target();
            String detail = "方法 " + point.getSignature().getName() + " 参数 " + describeArgs(point);
            String ip = ip();
            auditLogService.log(auditLog.module(), auditLog.action(), target, detail, ip);
        } catch (Exception ignore) {
            // 审计失败不能影响主业务
        }
        return result;
    }

    /** 参数摘要：仅取标量与常见 VO 的 toString 前 120 字（防敏感字段全量落库由上层 target 控制） */
    private String describeArgs(ProceedingJoinPoint point) {
        Object[] args = point.getArgs();
        if (args == null || args.length == 0) return "";
        return Arrays.stream(args)
                .filter(a -> a != null)
                .map(a -> {
                    String s = a.toString();
                    return s.length() > 60 ? s.substring(0, 60) + "…" : s;
                })
                .collect(Collectors.joining("; "));
    }

    private String ip() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
