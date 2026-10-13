-- ============================================================
-- 服务套餐模块（驱虫/体检/免疫套餐等）
-- 适用：MySQL 8 / MariaDB
-- 说明：套餐=可售卖的打包服务，计费走 bill.fee_package；内容为文本描述，不强制展开子订单
-- ============================================================
SET NAMES utf8mb4;

-- 1. 套餐字典
DROP TABLE IF EXISTS `service_package`;
CREATE TABLE `service_package` (
  `package_id` int NOT NULL AUTO_INCREMENT COMMENT '套餐id',
  `package_name` varchar(64) NOT NULL COMMENT '套餐名',
  `category` varchar(32) DEFAULT NULL COMMENT '分类：驱虫/体检/免疫/美容等',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '套餐价(元)',
  `content` varchar(255) DEFAULT NULL COMMENT '包含内容(文本描述)',
  `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '启停',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`package_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='服务套餐字典';

-- 2. 就诊选购的套餐记录
DROP TABLE IF EXISTS `record_package`;
CREATE TABLE `record_package` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `record_id` int NOT NULL COMMENT '病历id',
  `package_id` int DEFAULT NULL COMMENT '套餐id',
  `package_name` varchar(64) DEFAULT NULL COMMENT '冗余套餐名',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '成交价',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='就诊套餐记录';

-- 兼容旧库：补 update_time 列（实体有 @TableField(fill=INSERT_UPDATE)，缺列会导致插入报
-- "There is no setter for property named 'updateTime'"）
ALTER TABLE `record_package`
  ADD COLUMN `update_time` datetime DEFAULT NULL COMMENT '修改时间' AFTER `create_time`;

-- 3. 账单增加套餐费分项
ALTER TABLE `bill`
  ADD COLUMN `fee_package` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '套餐费(元)' AFTER `fee_image`;

-- 4. 预置常用套餐
INSERT INTO `service_package` (`package_name`, `category`, `price`, `content`) VALUES
('基础体检套餐', '体检', 99.00, '体格检查+血常规+体温心率'),
('全面体检套餐', '体检', 299.00, '体格检查+血常规+生化+影像'),
('犬用驱虫套餐', '驱虫', 80.00, '体内外驱虫+复查'),
('猫用驱虫套餐', '驱虫', 80.00, '体内外驱虫+复查'),
('幼宠免疫套餐', '免疫', 200.00, '首免疫苗+体检+驱虫');
