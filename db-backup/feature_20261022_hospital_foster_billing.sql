-- 住院/寄养计费体系（2026-10-22 走查整改）
-- ① 住院费用分项：日均费从"一口价"拆为 床位费/护理费/输液费 三项单价（daily_fee=三者之和，兼容旧数据）
-- ② 住院押金：预收登记 + 收取人/时间留痕，出院结算算应退
-- ③ 住院医嘱临时费用：住院期间追加的一次性治疗/用药费用
-- ④ 寄养按天计费：入托日期 + 笼位/清洁/餐费三项单价 + 状态流转 + 实际接回日期
-- 幂等：逐列判断已存在则跳过，可重复执行。

SET @db := DATABASE();

-- hospital_record：床位/护理/输液/押金/收取人/收取时间
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'hospital_record' AND COLUMN_NAME = 'bed_fee') > 0,
    'SELECT 1',
    'ALTER TABLE `hospital_record`
        ADD COLUMN `bed_fee` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''床位费/天'' AFTER `daily_fee`,
        ADD COLUMN `nursing_fee` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''护理费/天'' AFTER `bed_fee`,
        ADD COLUMN `infusion_fee` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''输液费/天'' AFTER `nursing_fee`,
        ADD COLUMN `deposit` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''已收押金(累加)'' AFTER `infusion_fee`,
        ADD COLUMN `deposit_by` varchar(32) NULL COMMENT ''最后押金收取人'' AFTER `deposit`,
        ADD COLUMN `deposit_time` datetime NULL COMMENT ''最后押金收取时间'' AFTER `deposit_by`'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- hospital_advice：医嘱临时费用
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'hospital_advice' AND COLUMN_NAME = 'fee') > 0,
    'SELECT 1',
    'ALTER TABLE `hospital_advice`
        ADD COLUMN `fee` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''临时费用(一次性，出院结算计入)'' AFTER `days`'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- foster：入托日期 + 三项单价 + 状态 + 实际接回日期
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'foster' AND COLUMN_NAME = 'in_date') > 0,
    'SELECT 1',
    'ALTER TABLE `foster`
        ADD COLUMN `in_date` date NULL COMMENT ''入托日期'' AFTER `pet_id`,
        ADD COLUMN `daily_fee` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''笼位费/天'' AFTER `foster_money`,
        ADD COLUMN `clean_fee` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''清洁费/天'' AFTER `daily_fee`,
        ADD COLUMN `meal_fee` decimal(10,2) NOT NULL DEFAULT 0 COMMENT ''餐费/天'' AFTER `clean_fee`,
        ADD COLUMN `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''0待入托 1寄养中 2已接回'' AFTER `meal_fee`,
        ADD COLUMN `back_date` date NULL COMMENT ''实际接回日期'' AFTER `status`'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 存量兼容：旧住院记录按原日均费拆一笔床位费（保证旧数据结算口径不变）
UPDATE `hospital_record` SET `bed_fee` = `daily_fee`
WHERE (`bed_fee` IS NULL OR `bed_fee` = 0) AND `daily_fee` > 0;
