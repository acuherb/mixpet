-- P0-1 会员储值流水表（2026-11-13，迁移硬阻塞①：无流水无法出对账单）
-- 适用于：已存在运行库（旧库）升级。全新部署直接导入 pet_mix.sql 即可（下次重压基线时并入）。
-- 可重复执行：表已存在时自动跳过。
-- 对账口径：Σ(amount) + 迁移期初 = client.client_balance 当前值。

CREATE TABLE IF NOT EXISTS `client_balance_log` (
    `log_id`         INT AUTO_INCREMENT COMMENT '流水id',
    `client_id`      INT           NOT NULL COMMENT '客户id',
    `change_type`    VARCHAR(32)   NOT NULL COMMENT 'RECHARGE充值/PAY收款扣款/REFUND退款回充/BUY_CARD购卡/POINTS_EXCHANGE积分兑换/MIGRATION_OPEN迁移期初',
    `amount`         DECIMAL(12,2) NOT NULL COMMENT '变动金额（带符号：正加负减）',
    `balance_before` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动前余额',
    `balance_after`  DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '变动后余额',
    `ref_type`       VARCHAR(32)   DEFAULT NULL COMMENT '关联单据类型(BILL/COUNT_CARD等)',
    `ref_id`         INT DEFAULT NULL COMMENT '关联单据id',
    `operator`       VARCHAR(64)   DEFAULT NULL COMMENT '操作人（登录态取，防伪造）',
    `note`           VARCHAR(255)  DEFAULT NULL COMMENT '备注（含充值返赠说明）',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_client_time` (`client_id`, `create_time`),
    KEY `idx_change_type` (`change_type`),
    KEY `idx_ref` (`ref_type`, `ref_id`)
) ENGINE = InnoDB COMMENT = '会员储值流水（余额每次变动一条，PetSystem→mixpet 迁移对账地基）';
