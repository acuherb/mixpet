-- ============================================================
-- 处方天数落库（农业部 734 号公告处方笺「天数」列）
-- 背景：医生开方界面早已录入 days（数量=单次剂量×频次×天数），
--       但 prescription 表无该列，保存即丢，打印页只能显示用法用量。
-- 2026-11-13
-- ============================================================
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'prescription' AND COLUMN_NAME = 'duration_days');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `prescription` ADD COLUMN `duration_days` INT DEFAULT NULL COMMENT ''用药天数（数量=单次剂量×频次×天数）'' AFTER `single_dose_unit`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
