-- ============================================================
-- 迁移追溯列（PetSystem 老单号 → 新表 legacy_*）
-- 作用：①ETL 幂等（重跑不重复插入，按 legacy 键 upsert）
--       ②对账（老单号 ↔ 新主键双向可查）
--       ③上线后客诉追溯（用户报老会员号/宠物号/病例号可直接定位）
-- 2026-11-13
-- ============================================================

-- client.legacy_member_no ← emember.MEMBER_ID（唯一）
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='client' AND COLUMN_NAME='legacy_member_no')=0,
    'ALTER TABLE `client` ADD COLUMN `legacy_member_no` VARCHAR(32) NULL COMMENT ''老系统会员号（迁移追溯）'', ADD UNIQUE KEY `uk_client_legacy_no` (`legacy_member_no`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- pet.legacy_pet_no ← epet.PET_ID（唯一）
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='pet' AND COLUMN_NAME='legacy_pet_no')=0,
    'ALTER TABLE `pet` ADD COLUMN `legacy_pet_no` VARCHAR(32) NULL COMMENT ''老系统宠物号（迁移追溯）'', ADD UNIQUE KEY `uk_pet_legacy_no` (`legacy_pet_no`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- medical_record.legacy_case_no ← epetcase.CASE_ID（不唯一索引：可能重跑覆盖）
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='medical_record' AND COLUMN_NAME='legacy_case_no')=0,
    'ALTER TABLE `medical_record` ADD COLUMN `legacy_case_no` VARCHAR(32) NULL COMMENT ''老系统病例号（迁移追溯）'', ADD KEY `idx_record_legacy_case` (`legacy_case_no`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- prescription.legacy_item_id ← epetcaseprescriptionitem.ITEM_ID（唯一）
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='prescription' AND COLUMN_NAME='legacy_item_id')=0,
    'ALTER TABLE `prescription` ADD COLUMN `legacy_item_id` VARCHAR(64) NULL COMMENT ''老系统处方明细ID（迁移追溯）'', ADD UNIQUE KEY `uk_prescription_legacy_item` (`legacy_item_id`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- quarantine_cert.legacy_no ← epetjianyi.JIANYI_ID（迁移进来的老检疫单）
SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='quarantine_cert' AND COLUMN_NAME='legacy_no')=0,
    'ALTER TABLE `quarantine_cert` ADD COLUMN `legacy_no` VARCHAR(32) NULL COMMENT ''老系统检疫单ID（迁移追溯）'', ADD UNIQUE KEY `uk_quarantine_legacy` (`legacy_no`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
