package com.mix.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mix.model.entity.AuditLog;
import com.mix.security.model.RoleConst;
import com.mix.service.IAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作审计日志查询（仅管理者以上：出纠纷时追溯“谁/何时/做了什么”）
 */
@RestController
@RequestMapping("/auditLog")
public class AuditLogController {
    private final IAuditLogService auditLogService;

    @Autowired
    public AuditLogController(IAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/page")
    @Secured({RoleConst.ADMIN, RoleConst.MANAGER})
    public IPage<AuditLog> page(int numPage, int pageSize, String operator, String module) {
        return auditLogService.page(new Page<>(numPage, pageSize), Wrappers.<AuditLog>lambdaQuery()
                .like(operator != null && !operator.isBlank(), AuditLog::getOperator, operator)
                .eq(module != null && !module.isBlank(), AuditLog::getModule, module)
                .orderByDesc(AuditLog::getLogId));
    }
}
