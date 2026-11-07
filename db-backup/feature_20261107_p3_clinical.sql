-- P3 临床合规与闭环（2026-11-07）
-- 适用于已有运行库升级；全新部署直接导入 pet_mix.sql（基线已含同样 DDL）。
-- 可重复执行：按「表」整组幂等（以该组第一列是否存在为准）；新表用 IF NOT EXISTS。
-- 依赖：先执行 P0（资金字段/审计表）与 P1（排班默认值/号源索引）。

-- ============ 1) 病历：版本号、完结电子签名、复核状态 ============
-- 变更前快照另存 medical_record_revision，病历本身不再被静默覆盖。
SET @ddl := (
    SELECT IF(COUNT(*) > 0, 'SELECT 1',
              'ALTER TABLE `medical_record`
                 ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT ''病历版本号'',
                 ADD COLUMN `signed_by` VARCHAR(64) NULL COMMENT ''完结签名医师'',
                 ADD COLUMN `signed_at` DATETIME NULL COMMENT ''签名时间'',
                 ADD COLUMN `review_status` TINYINT NOT NULL DEFAULT 0 COMMENT ''0待审核 1已审核'',
                 ADD COLUMN `review_by` VARCHAR(64) NULL COMMENT ''审核人'',
                 ADD COLUMN `review_at` DATETIME NULL COMMENT ''审核时间''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'medical_record' AND COLUMN_NAME = 'version');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 2) 化验单：标本与临床时间轴、复核、危急值、修订次数 ============
SET @ddl := (
    SELECT IF(COUNT(*) > 0, 'SELECT 1',
              'ALTER TABLE `lab_order`
                 ADD COLUMN `specimen_type` VARCHAR(32) NULL COMMENT ''标本类型'',
                 ADD COLUMN `collect_time` DATETIME NULL COMMENT ''采样时间'',
                 ADD COLUMN `receive_time` DATETIME NULL COMMENT ''标本接收时间'',
                 ADD COLUMN `review_by` VARCHAR(64) NULL COMMENT ''报告复核人'',
                 ADD COLUMN `review_at` DATETIME NULL COMMENT ''复核时间'',
                 ADD COLUMN `critical_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否含危急值'',
                 ADD COLUMN `result_revision` INT NOT NULL DEFAULT 0 COMMENT ''结果修订次数''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_order' AND COLUMN_NAME = 'specimen_type');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 3) 化验明细：开单时快照下来的数值化参考区间与危急阈值 ============
SET @ddl := (
    SELECT IF(COUNT(*) > 0, 'SELECT 1',
              'ALTER TABLE `lab_order_item`
                 ADD COLUMN `ref_low` DECIMAL(12,4) NULL COMMENT ''参考下限'',
                 ADD COLUMN `ref_high` DECIMAL(12,4) NULL COMMENT ''参考上限'',
                 ADD COLUMN `critical_low` DECIMAL(12,4) NULL COMMENT ''危急下限'',
                 ADD COLUMN `critical_high` DECIMAL(12,4) NULL COMMENT ''危急上限'',
                 ADD COLUMN `critical_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否命中危急值''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_order_item' AND COLUMN_NAME = 'ref_low');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============ 4) 病历版本留痕 ============
CREATE TABLE IF NOT EXISTS `medical_record_revision` (
    `revision_id` BIGINT NOT NULL AUTO_INCREMENT,
    `record_id` INT NOT NULL COMMENT '病历id',
    `revision_no` INT NOT NULL DEFAULT 1 COMMENT '变更前版本号',
    `snapshot_json` LONGTEXT COMMENT '变更前整本病历 JSON 快照',
    `editor_id` INT NULL COMMENT '操作人id',
    `editor_name` VARCHAR(64) NULL COMMENT '操作人姓名（取登录态）',
    `change_reason` VARCHAR(200) NULL COMMENT '变更原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`revision_id`),
    KEY `idx_mrr_record` (`record_id`, `revision_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病历修改留痕';

-- ============ 5) 按物种/性别/月龄分层的参考区间 ============
-- 注意：这里刻意不预置任何医学数值。参考区间与危急阈值随仪器、试剂、单位、物种而异，
-- 必须由本院检验科核对后录入；未配置时系统回退解析 lab_test.ref_range 的文本区间兼容旧单据。
CREATE TABLE IF NOT EXISTS `lab_ref_range` (
    `range_id` INT NOT NULL AUTO_INCREMENT,
    `test_id` INT NOT NULL COMMENT '检验项id',
    `species` VARCHAR(32) NULL COMMENT '物种：犬/猫；空=通用',
    `sex` VARCHAR(8) NULL COMMENT '公/母；空=不限',
    `age_min_month` INT NULL COMMENT '最小月龄（含）',
    `age_max_month` INT NULL COMMENT '最大月龄（含）',
    `ref_low` DECIMAL(12,4) NULL,
    `ref_high` DECIMAL(12,4) NULL,
    `critical_low` DECIMAL(12,4) NULL COMMENT '低于此值判危急',
    `critical_high` DECIMAL(12,4) NULL COMMENT '高于此值判危急',
    `ref_text` VARCHAR(64) NULL COMMENT '展示用文本，如 6.0-17.0',
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (`range_id`),
    KEY `idx_lrr_test` (`test_id`, `active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验参考区间（按物种/性别/月龄分层）';

-- ============ 6) 化验结果修订留痕 ============
CREATE TABLE IF NOT EXISTS `lab_result_revision` (
    `revision_id` BIGINT NOT NULL AUTO_INCREMENT,
    `order_id` INT NOT NULL COMMENT '化验单id',
    `revision_no` INT NOT NULL DEFAULT 0 COMMENT '变更前修订次数',
    `snapshot_json` LONGTEXT COMMENT '变更前整单快照（单头+全部明细）',
    `operator` VARCHAR(64) NULL,
    `change_reason` VARCHAR(200) NULL COMMENT '修订原因（必填）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`revision_id`),
    KEY `idx_lor_order` (`order_id`, `revision_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检验结果修订留痕';

-- ============ 7) 顾客定向站内通知 ============
CREATE TABLE IF NOT EXISTS `client_notification` (
    `notify_id` BIGINT NOT NULL AUTO_INCREMENT,
    `client_id` INT NOT NULL COMMENT '顾客id（查询强制按此过滤）',
    `title` VARCHAR(100) NOT NULL,
    `content` VARCHAR(500) NULL,
    `notify_type` VARCHAR(32) NULL COMMENT 'REPORT_LAB / REPORT_IMAGE / GENERAL',
    `ref_type` VARCHAR(32) NULL,
    `ref_id` INT NULL,
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`notify_id`),
    KEY `idx_cn_client` (`client_id`, `is_read`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客站内通知（报告出具主动触达）';
