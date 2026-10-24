-- 处方笺/收银留痕：账单增加收款操作人（收银者）
-- 背景：收银不限定角色（忙时医生可代收），但须记录谁收的，便于对账追责
-- 说明：历史已收账单 pay_by 为 NULL（前端显示「—（历史未记录）」），不影响列表

ALTER TABLE `bill`
    ADD COLUMN `pay_by` varchar(50) NULL COMMENT '收款操作人姓名（谁登录谁收款即记录）' AFTER `pay_type`;
