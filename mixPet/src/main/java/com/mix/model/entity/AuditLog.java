package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志（高危写操作留痕，可追溯）
 */
@TableName("audit_log")
public class AuditLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;
    /** 操作人姓名（登录态取） */
    private String operator;
    /** 操作角色 */
    private String role;
    /** 模块 */
    private String module;
    /** 动作 */
    private String action;
    /** 操作对象描述 */
    private String target;
    /** 详情 */
    private String detail;
    /** 来源IP */
    private String ip;
    /** 操作时间 */
    private LocalDateTime createTime;

    public Integer getLogId() { return logId; }

    public void setLogId(Integer logId) { this.logId = logId; }

    public String getOperator() { return operator; }

    public void setOperator(String operator) { this.operator = operator; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public String getModule() { return module; }

    public void setModule(String module) { this.module = module; }

    public String getAction() { return action; }

    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }

    public void setTarget(String target) { this.target = target; }

    public String getDetail() { return detail; }

    public void setDetail(String detail) { this.detail = detail; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public LocalDateTime getCreateTime() { return createTime; }

    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
