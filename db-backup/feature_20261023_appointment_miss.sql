-- 挂号爽约改状态位（2026-10-23 走查整改）
-- 原实现：医生标记爽约 = 逻辑删除（is_del=时间戳）→ 顾客端预约凭空消失、无任何记录
-- 新实现：miss 状态位（0正常 1爽约），记录保留可查，顾客端显示「已爽约」，闭环不可再改期/取消
ALTER TABLE `appointment`
    ADD COLUMN `miss` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0正常 1爽约(医生标记，记录保留不删)' AFTER `is_suc`;

-- 存量说明：此前被逻辑删除的爽约单不恢复（is_del 有值的历史数据），新逻辑自此生效
