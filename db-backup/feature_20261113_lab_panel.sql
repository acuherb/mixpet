-- 化验套组 feature_20261113_lab_panel
-- 背景：宠物医院很多化验是成套试剂盘（如生化30项），应按套组一次性收费(开单价)，
--       但下面有 N 个成员子项需要逐一录入结果；当前模型按单项逐一收费会重复计费。
-- 本脚本：① lab_test 增加套组标识/开单价/进价；② 新建套组-成员映射表；
--       ③ lab_order_item 增加所属套组；④ lab_order 增加成本(套组进价合计)用于毛利。
-- 幂等：用 information_schema 判断列是否存在，可重复执行。

USE pet_mix;

-- ① lab_test 套组相关列
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_test' AND COLUMN_NAME = 'is_panel');
SET @sql = IF(@s = 0,
  "ALTER TABLE lab_test
     ADD COLUMN is_panel TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否套组：1=套组(只收一次费) 0=单项',
     ADD COLUMN panel_price DECIMAL(10,2) DEFAULT NULL COMMENT '套组开单价(元)，is_panel=1 时使用',
     ADD COLUMN panel_cost DECIMAL(10,2) DEFAULT NULL COMMENT '套组进价/试剂盘成本(元)，用于毛利'",
  "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ② 套组-成员映射表（套组与单项多对多，含排序）
CREATE TABLE IF NOT EXISTS lab_panel_member (
  panel_id      INT(11) NOT NULL COMMENT '套组id(lab_test.test_id, is_panel=1)',
  member_test_id INT(11) NOT NULL COMMENT '成员单项id(lab_test.test_id, is_panel=0)',
  sort_order    INT(11) NOT NULL DEFAULT 0 COMMENT '成员排序',
  PRIMARY KEY (panel_id, member_test_id),
  KEY idx_lpm_member (member_test_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '化验套组-成员映射';

-- ③ 化验单明细：所属套组（null=独立单项，避免重复计费）
SET @s2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_order_item' AND COLUMN_NAME = 'panel_id');
SET @sql2 = IF(@s2 = 0,
  "ALTER TABLE lab_order_item ADD COLUMN panel_id INT(11) DEFAULT NULL COMMENT '所属套组id(lab_test.test_id)，null=独立单项'",
  "SELECT 1");
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 化验单明细：套组名称快照（开单时冗余，便于结果/报告按套组分组展示，无需再查字典）
SET @s4 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_order_item' AND COLUMN_NAME = 'panel_name');
SET @sql4 = IF(@s4 = 0,
  "ALTER TABLE lab_order_item ADD COLUMN panel_name VARCHAR(64) DEFAULT NULL COMMENT '所属套组名称快照，null=独立单项'",
  "SELECT 1");
PREPARE stmt4 FROM @sql4; EXECUTE stmt4; DEALLOCATE PREPARE stmt4;

-- ④ 化验单：成本（套组进价合计），用于毛利
SET @s3 = (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'lab_order' AND COLUMN_NAME = 'cost');
SET @sql3 = IF(@s3 = 0,
  "ALTER TABLE lab_order ADD COLUMN cost DECIMAL(10,2) DEFAULT NULL COMMENT '化验成本(元)=套组进价合计，用于毛利'",
  "SELECT 1");
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;
