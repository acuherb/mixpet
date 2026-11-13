-- ============================================================
-- 检疫单（PetSystem ReportPris_jianyi 版式移植，2026-11-13）
-- 用途：宠物出行/托运等场景由医院开具的检疫证明，含检疫清单与文字记录；
--       单号 JY-YYYYMMDD-XXXX 唯一，落库留痕（补打/追溯）。
-- ============================================================
SET @tbl_exists := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'quarantine_cert');
SET @ddl := IF(@tbl_exists = 0,
    'CREATE TABLE `quarantine_cert` (
        `cert_id` INT NOT NULL AUTO_INCREMENT,
        `cert_no` VARCHAR(32) NOT NULL COMMENT ''检疫单号 JY-YYYYMMDD-XXXX'',
        `record_id` INT NOT NULL COMMENT ''关联病历'',
        `pet_id` INT DEFAULT NULL,
        `client_id` INT DEFAULT NULL,
        `doctor_id` INT DEFAULT NULL COMMENT ''开单人'',
        `owner_name` VARCHAR(64) DEFAULT NULL COMMENT ''主人名称/饲养单位（快照）'',
        `owner_tel` VARCHAR(32) DEFAULT NULL COMMENT ''联系电话（快照）'',
        `pet_name` VARCHAR(64) DEFAULT NULL COMMENT ''动物名称（快照）'',
        `species` VARCHAR(32) DEFAULT NULL COMMENT ''动物种类（犬/猫…）'',
        `breed` VARCHAR(64) DEFAULT NULL COMMENT ''品种'',
        `sex` VARCHAR(8) DEFAULT NULL COMMENT ''性别'',
        `age_desc` VARCHAR(32) DEFAULT NULL COMMENT ''年龄描述（如 2岁3月）'',
        `weight` DECIMAL(8,3) DEFAULT NULL COMMENT ''体重kg'',
        `content` VARCHAR(500) DEFAULT NULL COMMENT ''文字记录/检疫结论'',
        `items_json` TEXT DEFAULT NULL COMMENT ''检疫清单快照（药品/项目数组 JSON）'',
        `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
        `is_del` BIGINT NOT NULL DEFAULT 0,
        PRIMARY KEY (`cert_id`),
        UNIQUE KEY `uk_cert_no` (`cert_no`),
        KEY `idx_record` (`record_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT=''检疫单（含清单与结论，医院开具留痕）''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
