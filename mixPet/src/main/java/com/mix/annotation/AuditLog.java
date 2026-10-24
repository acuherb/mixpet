package com.mix.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计注解：贴在需要留痕的高危写操作方法上，由 AuditLogAspect 统一记录
 * （操作人从登录态取，不信任任何参数——防伪造）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    /** 模块：bill/client/drug/backup/record... */
    String module();

    /** 动作：pay/refund/dispense/audit/inbound/restore... */
    String action();

    /** 操作对象描述（支持 SpEL 暂不引入，直接给静态文案或用 #args 由切面拼） */
    String target() default "";
}
