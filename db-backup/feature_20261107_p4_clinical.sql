-- P4 临床专业度深化（2026-11-07）
-- 适用于已有运行库升级；全新部署直接导入 pet_mix.sql（基线已含同样 DDL）。
-- 可重复执行：列用 information_schema 幂等；新表用 IF NOT EXISTS；诊断字典初值用 IGNORE 幂等插入。
-- 依赖：先执行 P0、P1、P3（本脚本在 P3 之上追加列与表）。

-- ============ 1) 病历：SOAP 的 A 结构化（鉴别诊断/诊断依据/编码） + 审核退回理由 ============
SET @ddl := (
    SELECT IF(COUNT(*) > 0, 'SELECT 1',
              'ALTER TABLE `medical_record`
                 ADD COLUMN `differential_diagnosis` TEXT NULL COMMENT ''鉴别诊断'',
                 ADD COLUMN `diagnosis_basis` TEXT NULL COMMENT ''诊断依据'',
                 ADD COLUMN `diagnosis_code` VARCHAR(64) NULL COMMENT ''诊断编码（快照字典的 code）'',
                 ADD COLUMN `diagnosis_code_system` VARCHAR(32) NULL COMMENT ''编码体系 INTERNAL/ICD11_VET...'',
                 ADD COLUMN `review_note` VARCHAR(200) NULL COMMENT ''审核退回理由（修改后清空）''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical_record' AND COLUMN_NAME = 'differential_diagnosis');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 2) 影像报告结构化：所见/诊断分离 + DICOM 元数据 + 原图附件 ============
SET @ddl := (
    SELECT IF(COUNT(*) > 0, 'SELECT 1',
              'ALTER TABLE `image_order`
                 ADD COLUMN `findings` TEXT NULL COMMENT ''影像所见（客观描述）'',
                 ADD COLUMN `impression` TEXT NULL COMMENT ''影像诊断（结论）'',
                 ADD COLUMN `dicom_uid` VARCHAR(128) NULL COMMENT ''DICOM Study UID'',
                 ADD COLUMN `dicom_series` VARCHAR(128) NULL COMMENT ''DICOM Series UID'',
                 ADD COLUMN `image_files` TEXT NULL COMMENT ''原图附件文件名 JSON 数组''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'image_order' AND COLUMN_NAME = 'findings');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 3) 诊断编码字典（SOAP 的 A：结构化编码 + 体系） ============
CREATE TABLE IF NOT EXISTS `diagnosis_dict` (
    `dict_id` INT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(64) NOT NULL COMMENT '编码（院内码 / ICD-11-VET 等）',
    `name` VARCHAR(128) NOT NULL COMMENT '诊断名称',
    `code_system` VARCHAR(32) NULL COMMENT 'INTERNAL / ICD11_VET / SNOMED_CT_VET',
    `category` VARCHAR(64) NULL COMMENT '分类（内科/外科...）',
    `species` VARCHAR(32) NULL COMMENT '适用物种：犬/猫；空=通用',
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`dict_id`),
    UNIQUE KEY `uk_dd_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诊断编码字典';

-- 初值（幂等，不覆盖既有）：以院内码为主，便于后续对接 ICD-11-VET。
INSERT IGNORE INTO `diagnosis_dict` (`code`, `name`, `code_system`, `category`, `species`) VALUES
('D-ENTER', '胃肠炎', 'INTERNAL', '消化内科', NULL),
('D-CATAR', '猫泛白细胞减少症', 'INTERNAL', '传染病', '猫'),
('D-DIST', '犬细小病毒病', 'INTERNAL', '传染病', '犬'),
('D-FLU', '犬瘟热', 'INTERNAL', '传染病', '犬'),
('D-URI', '猫上呼吸道综合征', 'INTERNAL', '传染病', '猫'),
('D-CKD', '慢性肾病', 'INTERNAL', '泌尿内科', NULL),
('D-FLUTD', '猫下泌尿道综合征', 'INTERNAL', '泌尿内科', '猫'),
('D-DM', '糖尿病', 'INTERNAL', '内分泌', NULL),
('D-OBS', '肥胖症', 'INTERNAL', '营养代谢', NULL),
('D-DEN', '牙结石/牙周病', 'INTERNAL', '口腔科', NULL),
('D-OTI', '外耳道炎', 'INTERNAL', '皮肤科', NULL),
('D-DERM', '异位性皮炎', 'INTERNAL', '皮肤科', NULL),
('D-ARTH', '骨关节炎', 'INTERNAL', '骨科', NULL),
('D-PLL', '前十字韧带断裂', 'INTERNAL', '骨科', NULL),
('D-MASS', '皮下肿物', 'INTERNAL', '外科', NULL),
('D-PYO', '子宫蓄脓', 'INTERNAL', '产科', '母'),
('D-GAST', '胃扩张扭转综合征(GDV)', 'INTERNAL', '外科', '犬'),
('D-ANEM', '贫血', 'INTERNAL', '血液内科', NULL),
('D-UTI', '泌尿道感染', 'INTERNAL', '泌尿内科', NULL),
('D-RESP', '心源性/呼吸窘迫', 'INTERNAL', '心肺内科', NULL);

-- ============ 4) 化验危急值处置闭环 ============
SET @ddl := (
    SELECT IF(COUNT(*) > 0, 'SELECT 1',
              'ALTER TABLE `lab_order`
                 ADD COLUMN `critical_handled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''危急值是否已处置'',
                 ADD COLUMN `critical_handle_by` VARCHAR(64) NULL COMMENT ''处置人'',
                 ADD COLUMN `critical_handle_at` DATETIME NULL COMMENT ''处置时间'',
                 ADD COLUMN `critical_notify_at` DATETIME NULL COMMENT ''通知接诊医生时间''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_order' AND COLUMN_NAME = 'critical_handled');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
