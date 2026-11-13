-- step 2 ETL 前置（2026-11-13）
-- bill.legacy_bill_no：PetSystem epetcaseinvoice.INVOICE_NUM → bill 迁移追溯列
-- 作用：①ETL 幂等（按 legacy 键 upsert）②对账（老收费单号 ↔ 新 bill_id）③客诉追溯
-- 风格沿用 feature_20261113_legacy_keys.sql；可重复执行。

SET @s := IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='bill' AND COLUMN_NAME='legacy_bill_no')=0,
    'ALTER TABLE `bill` ADD COLUMN `legacy_bill_no` VARCHAR(32) NULL COMMENT ''老系统收费单号（迁移追溯）'', ADD UNIQUE KEY `uk_bill_legacy_no` (`legacy_bill_no`)',
    'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
