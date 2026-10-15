-- 2026-10-15：经营财务报表——药品进货价(成本价)
ALTER TABLE drug ADD COLUMN cost_price DECIMAL(10,2) NULL COMMENT '进货价(成本价)，用于药品毛利核算';
