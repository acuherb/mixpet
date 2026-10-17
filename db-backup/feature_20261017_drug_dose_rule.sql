-- 药品剂量规则：处方按体重/固定量自动算量（对标小弗智能开单）
-- dose_per_kg：每kg体重单次用量（单位与药剂单位一致，如 0.1 表示 0.1mL/kg）
-- dose_fixed：固定单次用量（与体重无关，优先于 dose_per_kg）
-- 两者均为空 = 不自动算量，维持手动填写
-- 适用数据库：pet_ship

ALTER TABLE `drug`
    ADD COLUMN `dose_per_kg` decimal(10, 3) NULL DEFAULT NULL COMMENT '每kg体重单次用量(单位同药剂单位)' AFTER `interval_days`,
    ADD COLUMN `dose_fixed` decimal(10, 2) NULL DEFAULT NULL COMMENT '固定单次用量(与体重无关，优先)' AFTER `dose_per_kg`;
