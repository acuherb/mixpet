-- ============================================================
-- 将疫苗字典合并进药品字典（疫苗 = 药品分类 drug_type='疫苗'）
-- 适用：MySQL 8 / MariaDB
-- 说明：一次性迁移。drug 增加 brand/prevent/interval_days（可空，仅疫苗用）；
--       vaccine 数据迁入 drug；immune_record 外键由 vaccine_id 改指 drug_id；删除 vaccine 表。
-- ============================================================
SET NAMES utf8mb4;

-- 1. drug 增加疫苗特有字段（可空，仅 drug_type=疫苗 使用）
ALTER TABLE `drug`
  ADD COLUMN `brand` varchar(64) DEFAULT NULL COMMENT '品牌/厂家(疫苗)' AFTER `drug_spec`,
  ADD COLUMN `prevent` varchar(128) DEFAULT NULL COMMENT '预防疾病(疫苗)' AFTER `brand`,
  ADD COLUMN `interval_days` int DEFAULT NULL COMMENT '建议接种间隔天数(疫苗，用于推算下一针)' AFTER `prevent`;

-- 2. 疫苗数据迁入药品；用临时列记住来源 vaccine_id，供回填免疫档案
ALTER TABLE `drug` ADD COLUMN `_src_vaccine_id` int DEFAULT NULL COMMENT '迁移临时列';
INSERT INTO `drug`
  (`drug_name`, `drug_spec`, `drug_unit`, `drug_type`, `drug_price`, `drug_stock`, `drug_info`,
   `active`, `brand`, `prevent`, `interval_days`, `is_del`, `_src_vaccine_id`)
SELECT `vaccine_name`, '', '支', '疫苗', `price`, 0,
       CONCAT('预防：', IFNULL(`prevent`, '')), 1, `brand`, `prevent`, `interval_days`,
       IFNULL(`is_del`, 0), `vaccine_id`
FROM `vaccine`;

-- 3. 免疫档案外键从 vaccine_id/vaccine_name 改指 drug_id/drug_name
ALTER TABLE `immune_record`
  CHANGE COLUMN `vaccine_id` `drug_id` int NOT NULL COMMENT '疫苗药品id(关联drug, drug_type=疫苗)',
  CHANGE COLUMN `vaccine_name` `drug_name` varchar(64) DEFAULT NULL COMMENT '冗余疫苗药品名';

-- 4. 回填免疫档案的 drug_id（按临时映射：旧 vaccine_id -> 新 drug_id）
UPDATE `immune_record` ir
  JOIN `drug` d ON ir.`drug_id` = d.`_src_vaccine_id`
SET ir.`drug_id` = d.`drug_id`;

-- 5. 清理临时列，删除旧疫苗表
ALTER TABLE `drug` DROP COLUMN `_src_vaccine_id`;
DROP TABLE IF EXISTS `vaccine`;
