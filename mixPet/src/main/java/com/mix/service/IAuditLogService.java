package com.mix.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mix.model.entity.AuditLog;

/**
 * 操作审计日志 Service
 */
public interface IAuditLogService extends IService<AuditLog> {
    /**
     * 记录一条审计日志（操作人/角色从登录态取，不信任参数）
     *
     * @param module 模块
     * @param action 动作
     * @param target 操作对象描述（如 账单#12）
     * @param detail 详情
     * @param ip     来源IP
     */
    void log(String module, String action, String target, String detail, String ip);
}
