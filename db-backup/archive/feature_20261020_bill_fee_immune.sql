-- 免疫接种计费入账单（方案B：疫苗与临床主链路打通）
-- 背景：到店直接打疫苗原先不进账单，收银台收不到这笔钱；现按疫苗价格计入 fee_immune
-- 入账规则：免疫登记时按 drug.drugPrice 累加进该病历「未支付」账单；无未付账单则新建
-- 疫苗即时出库（接种即消耗），库存不足拒绝登记（原子扣减防并发）

ALTER TABLE `bill`
    ADD COLUMN `fee_immune` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '疫苗费（免疫接种计入）' AFTER `fee_surgery`;
