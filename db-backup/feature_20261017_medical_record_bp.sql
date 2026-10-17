-- 病历体征补充血压字段（对标规范病历：体温/心率/呼吸/血压/体重五大体征）
-- 适用数据库：pet_ship

ALTER TABLE `medical_record`
    ADD COLUMN `blood_pressure` varchar(16) NULL DEFAULT NULL COMMENT '血压(收缩/舒张,如120/80)' AFTER `respiratory_rate`;
