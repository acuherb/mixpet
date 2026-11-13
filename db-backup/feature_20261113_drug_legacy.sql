-- P1-3 效期/追溯（2026-11-13，step 2 ETL 前置）
-- drug 增加 legacy_part_no：PetSystem epbmaster.PART_NUM → drug 迁移追溯列
-- 作用：①ETL 幂等（按 legacy 键 upsert，重跑不重复插）
--       ②对账（老料号 ↔ 新 drug_id 双向可查）
--       ③上线后客诉追溯（老处方/化验结果都指向 PART_NUM）
-- 风格沿用 feature_20261113_legacy_keys.sql；可重复执行。

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='drug' AND COLUMN_NAME='legacy_part_no')=0,
    'ALTER TABLE `drug` ADD COLUMN `legacy_part_no` VARCHAR(32) NULL COMMENT ''老系统料号（迁移追溯）'', ADD UNIQUE KEY `uk_drug_legacy_part` (`legacy_part_no`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
