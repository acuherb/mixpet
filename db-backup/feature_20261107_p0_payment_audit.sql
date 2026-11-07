-- P0 资金一致性 + 审计表（2026-11-07）
-- 适用于：已存在运行库（旧库）升级。全新部署直接导入 pet_mix.sql 即可（基线末尾已含同样 DDL）。
-- 可重复执行：列已存在时自动跳过。

-- 1) 独立收款时间：退款/追加处方/出院计费都会刷新 update_time，不能再拿它当日结口径
SET @ddl := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `bill` ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT ''收款时间（独立于 update_time）''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bill' AND COLUMN_NAME = 'pay_time');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) 累计已退金额（实付口径）：账单原始金额不再被退款冲减，报表按 real_money - refund_money 计算
SET @ddl := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `bill` ADD COLUMN `refund_money` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT ''累计已退金额（实付口径）''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bill' AND COLUMN_NAME = 'refund_money');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 回填历史已付账单的收款时间（仅一次，值为当时的 update_time）
UPDATE `bill` SET `pay_time` = `update_time` WHERE `pay_status` = 1 AND `pay_time` IS NULL;

-- 4) 审计日志表：不在 43 张业务表基线内时补齐，否则审计写入会被切面静默丢弃
CREATE TABLE IF NOT EXISTS `audit_log` (
    `log_id`      INT AUTO_INCREMENT COMMENT '日志id',
    `operator`    VARCHAR(64)  NOT NULL COMMENT '操作人（登录态取，防伪造）',
    `role`        VARCHAR(32)  DEFAULT NULL COMMENT '操作角色',
    `module`      VARCHAR(32)  NOT NULL COMMENT '模块',
    `action`      VARCHAR(64)  NOT NULL COMMENT '动作',
    `target`      VARCHAR(128) DEFAULT NULL COMMENT '操作对象',
    `detail`      VARCHAR(512) DEFAULT NULL COMMENT '详情（含成功/失败状态）',
    `ip`          VARCHAR(64)  DEFAULT NULL COMMENT '来源IP（直连地址）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_operator` (`operator`),
    KEY `idx_module_action` (`module`, `action`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB COMMENT = '操作审计日志';
