-- 2026-10-15：会员折扣接入收费——账单折扣/优惠/实收
-- discount 为普通列（建单时按客户等级快照）；
-- discount_money / real_money 为生成列，随 total_money 自动折算，
-- 因此"追加处方/套餐/化验/影像/手术/住院、作废冲减"等各处只需改 total_money，折后应付永远一致。
ALTER TABLE bill ADD COLUMN discount DECIMAL(3,2) NULL COMMENT '会员折扣率快照(1=不打折)';
ALTER TABLE bill ADD COLUMN discount_money DECIMAL(12,2) AS (ROUND(total_money * (1 - IFNULL(discount, 1)), 2)) PERSISTENT COMMENT '优惠金额';
ALTER TABLE bill ADD COLUMN real_money     DECIMAL(12,2) AS (ROUND(total_money * IFNULL(discount, 1), 2)) PERSISTENT COMMENT '折后应付/实收';

-- 存量账单：折扣率置 1（不打折），实收=原价
UPDATE bill SET discount = 1 WHERE discount IS NULL;
