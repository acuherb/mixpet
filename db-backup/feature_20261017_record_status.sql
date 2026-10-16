-- 就诊状态：medical_record 增加 status/finish_time/cancel_reason
-- 适用数据库：pet_ship
-- 说明：status 0接诊中 1已完结 2已作废；新接诊默认 0，完结/作废后置位并记录时间/原因
--       历史数据视为已完结（finish_time 取原 update_time），不影响既有查询

ALTER TABLE `medical_record`
    ADD COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '就诊状态：0接诊中 1已完结 2已作废' AFTER `triage_level`,
    ADD COLUMN `finish_time` datetime NULL DEFAULT NULL COMMENT '完结时间' AFTER `status`,
    ADD COLUMN `cancel_reason` varchar(255) NULL DEFAULT NULL COMMENT '作废原因' AFTER `finish_time`;

-- 历史数据默认已完结
UPDATE `medical_record` SET `status` = 1, `finish_time` = `update_time` WHERE `status` = 0;
