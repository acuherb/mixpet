-- 药品入库成本价（利润核算闭环）
-- 背景：报表已有药品毛利(按 drug.cost_price 当前进货价估算)，但入库流程不录成本 → 成本价无人维护 → 利润算不出
-- 改造：入库台账记每批成本价(留痕)；入库时同步刷新 drug.cost_price(当前成本=最新入库成本)

ALTER TABLE `drug_record`
    ADD COLUMN `cost_price` decimal(10, 2) NULL COMMENT '本批入库成本价(元/单位)' AFTER `batch_no`;
