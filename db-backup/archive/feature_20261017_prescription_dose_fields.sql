-- 处方结构化用药字段：单次剂量/频次（供处方笺打印与智能算量）
-- 数量(quantity)= ceil(单次剂量 × 频次 × 天数)，天数仅参与前端计算不入库
-- 适用数据库：pet_mix

ALTER TABLE `prescription`
    ADD COLUMN `daily_times` int(11) NULL DEFAULT NULL COMMENT '频次：每日几次' AFTER `quantity`,
    ADD COLUMN `single_dose` decimal(10, 3) NULL DEFAULT NULL COMMENT '单次剂量(单位同药剂单位)' AFTER `daily_times`,
    ADD COLUMN `single_dose_unit` varchar(16) NULL DEFAULT NULL COMMENT '单次剂量单位(片/mL/袋等)' AFTER `single_dose`;
