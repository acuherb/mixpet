-- 操作审计日志（2026-10-24）
-- 目的：高危写操作留痕（谁/何时/做了什么/针对哪条数据），出纠纷可追溯。
-- 覆盖：收费/退款/发药/顾客审核/库存出入/备份还原/处方作废等（接口贴 @AuditLog）。
CREATE TABLE IF NOT EXISTS `audit_log` (
    `log_id`     INT AUTO_INCREMENT COMMENT '日志id',
    `operator`   VARCHAR(64)  NOT NULL COMMENT '操作人姓名（登录态取，防伪造）',
    `role`       VARCHAR(32)  DEFAULT NULL COMMENT '操作角色（ADMIN/MANAGER/NURSE/DOCTOR）',
    `module`     VARCHAR(32)  NOT NULL COMMENT '模块（bill/backup/client/drug/record...）',
    `action`     VARCHAR(64)  NOT NULL COMMENT '动作（pay/refund/dispense/audit/restore...）',
    `target`     VARCHAR(128) DEFAULT NULL COMMENT '操作对象（如 账单#12 / 顾客#8）',
    `detail`     VARCHAR(512) DEFAULT NULL COMMENT '详情（方法可读的参数摘要）',
    `ip`         VARCHAR(64)  DEFAULT NULL COMMENT '来源IP',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_operator` (`operator`),
    KEY `idx_module_action` (`module`, `action`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB COMMENT ='操作审计日志';
