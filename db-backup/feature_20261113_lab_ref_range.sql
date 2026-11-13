-- ============================================================
-- lab_ref_range 化验参考值结构化表（PetSystem 迁移 step 2 落地件）
-- 依据 FIELD_MAPPING.md §2.7.2（P0-5 定案 2026-11-13）
-- 老参考值在 epbmaster 的 DOG/DOG_Y/DOG2..4 / CAT 系列等列，
-- 按 species + 月龄段拆行；药品/用品行借用同列存剂量，必须
-- 由 ETL 按 PART_NUM 前缀（y*/DEV*）过滤，本表不管过滤。
-- 注意：本表在部分环境已存在旧结构（range_id 主键、无追溯列），
-- 本脚本幂等：缺表则建全结构，已有表则只补缺失列/放宽约束。
-- ============================================================

-- 幂等建表（新环境）
SET @tbl_exists := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_ref_range');
SET @ddl := IF(@tbl_exists = 0,
    'CREATE TABLE `lab_ref_range` (
        `id` INT NOT NULL AUTO_INCREMENT,
        `test_id` INT DEFAULT NULL COMMENT ''关联 lab_test.test_id（ETL 按名称映射后回填）'',
        `legacy_no` VARCHAR(32) DEFAULT NULL COMMENT ''老系统料号 PART_NUM（追溯用）'',
        `species` VARCHAR(8) NOT NULL COMMENT ''犬/猫/兔/仓鼠/龙猫'',
        `sex` VARCHAR(4) DEFAULT NULL COMMENT ''公/母/NULL=不限（老系统无此维度，预留）'',
        `age_min_month` INT DEFAULT NULL COMMENT ''月龄下限，NULL=不限'',
        `age_max_month` INT DEFAULT NULL COMMENT ''月龄上限，NULL=不限'',
        `ref_low` DECIMAL(12,4) DEFAULT NULL COMMENT ''参考下限'',
        `ref_high` DECIMAL(12,4) DEFAULT NULL COMMENT ''参考上限'',
        `critical_low` DECIMAL(12,4) DEFAULT NULL COMMENT ''危急低值（老系统无，预留）'',
        `critical_high` DECIMAL(12,4) DEFAULT NULL COMMENT ''危急高值（老系统无，预留）'',
        `ref_text` VARCHAR(255) DEFAULT NULL COMMENT ''定性结果/无法拆分时保留原文'',
        `unit` VARCHAR(32) DEFAULT NULL COMMENT ''单位（老系统 UOM）'',
        `active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''启停'',
        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',
        PRIMARY KEY (`id`),
        KEY `idx_test` (`test_id`),
        KEY `idx_legacy` (`legacy_no`),
        KEY `idx_species` (`species`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT=''化验参考值（按物种/月龄段拆行，PetSystem 迁移）''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 已存在旧结构的库：补列（幂等）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_ref_range' AND COLUMN_NAME = 'legacy_no');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `lab_ref_range` ADD COLUMN `legacy_no` VARCHAR(32) DEFAULT NULL COMMENT ''老系统料号 PART_NUM（追溯用）'' AFTER `test_id`, ADD KEY `idx_legacy` (`legacy_no`)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_ref_range' AND COLUMN_NAME = 'unit');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `lab_ref_range` ADD COLUMN `unit` VARCHAR(32) DEFAULT NULL COMMENT ''单位（老系统 UOM）'' AFTER `ref_text`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_ref_range' AND COLUMN_NAME = 'create_time');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `lab_ref_range` ADD COLUMN `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `active`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ref_text 旧结构 64 长度不够（定性选项拼接可达 100+），扩到 255（幂等：MODIFY 重复执行无害）
SET @ddl := IF((SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_ref_range' AND COLUMN_NAME = 'ref_text') < 255,
    'ALTER TABLE `lab_ref_range` MODIFY `ref_text` VARCHAR(255) DEFAULT NULL COMMENT ''定性结果/无法拆分时保留原文''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 旧结构 test_id NOT NULL：ETL 未映射/纯定性行需允许 NULL（幂等）
SET @ddl := IF((SELECT IS_NULLABLE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_ref_range' AND COLUMN_NAME = 'test_id') = 'NO',
    'ALTER TABLE `lab_ref_range` MODIFY `test_id` INT DEFAULT NULL COMMENT ''关联 lab_test.test_id（ETL 按名称映射后回填）''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 兼容旧主键名：无 id 列（主键叫 range_id）的环境不动主键，ETL 不依赖主键名
