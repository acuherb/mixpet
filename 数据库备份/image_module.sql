-- ============================================================
-- 影像检查模块 新建表
-- 适用：MySQL 8 / MariaDB
-- 说明：影像项目字典 + 申请单（开单计费入 bill.fee_image，随收费列表结算）
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 影像项目字典
-- ----------------------------
DROP TABLE IF EXISTS `image_test`;
CREATE TABLE `image_test` (
  `item_id` int NOT NULL AUTO_INCREMENT COMMENT '项目id',
  `item_name` varchar(64) NOT NULL COMMENT '项目名称',
  `group_name` varchar(32) DEFAULT NULL COMMENT '分组（X光/B超/CT等）',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单价(元)',
  `detail` varchar(255) DEFAULT NULL COMMENT '说明',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`item_id`),
  KEY `idx_group` (`group_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='影像检查项目字典';

-- ----------------------------
-- 影像申请单（一个申请单对应一条项目）
-- ----------------------------
DROP TABLE IF EXISTS `image_order`;
CREATE TABLE `image_order` (
  `order_id` int NOT NULL AUTO_INCREMENT COMMENT '申请单id',
  `record_id` int NOT NULL COMMENT '病历id',
  `pet_id` int NOT NULL COMMENT '宠物id',
  `client_id` int NOT NULL COMMENT '客户id',
  `doctor_id` int NOT NULL COMMENT '开单医生id',
  `item_id` int NOT NULL COMMENT '项目id',
  `item_name` varchar(64) DEFAULT NULL COMMENT '冗余项目名（不受字典修改影响）',
  `group_name` varchar(32) DEFAULT NULL COMMENT '冗余分组',
  `fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '检查费用(元)',
  `exam_part` varchar(64) DEFAULT NULL COMMENT '检查部位/检查目的',
  `result` text COMMENT '影像报告内容',
  `report_time` datetime DEFAULT NULL COMMENT '出报告时间',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0待检查 1已出报告',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`order_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='影像检查申请单';

-- ----------------------------
-- 预置常用影像项目
-- ----------------------------
INSERT INTO `image_test` (`item_name`, `group_name`, `price`, `detail`) VALUES
('胸部X光片', 'X光', 60.00, '检查心肺胸腔'),
('腹部X光片', 'X光', 60.00, '检查腹腔脏器/异物'),
('四肢X光片', 'X光', 80.00, '骨折/关节检查'),
('腹部B超', 'B超', 100.00, '腹腔内脏器超声检查'),
('心脏彩超', 'B超', 150.00, '心脏结构与血流评估'),
('头部CT', 'CT', 300.00, '颅脑断层扫描'),
('胸腹部CT', 'CT', 400.00, '胸腹联合断层扫描');

-- ----------------------------
-- 账单表新增影像费字段
-- ----------------------------
ALTER TABLE `bill`
  ADD COLUMN `fee_image` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '影像费(元)' AFTER `fee_hospital`;