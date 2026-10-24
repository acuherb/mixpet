-- ============================================================
-- 化验/检验管理模块 新建表
-- 适用：MySQL 8 / MariaDB
-- 说明：化验费用计入 bill.fee_lab，结算走现有收费列表
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 检验项目字典
-- ----------------------------
DROP TABLE IF EXISTS `lab_test`;
CREATE TABLE `lab_test` (
  `test_id` int NOT NULL AUTO_INCREMENT COMMENT '检验项id',
  `test_name` varchar(64) NOT NULL COMMENT '项目名称',
  `test_group` varchar(32) DEFAULT NULL COMMENT '分组：血常规/生化/尿检/镜检',
  `test_unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `ref_range` varchar(64) DEFAULT NULL COMMENT '参考范围',
  `test_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单价(元)',
  `active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '启停，1启用0停用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`test_id`),
  KEY `idx_group` (`test_group`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='检验项目字典';

-- ----------------------------
-- 2. 化验单主表
-- ----------------------------
DROP TABLE IF EXISTS `lab_order`;
CREATE TABLE `lab_order` (
  `order_id` int NOT NULL AUTO_INCREMENT COMMENT '化验单id',
  `record_id` int NOT NULL COMMENT '病历id',
  `pet_id` int NOT NULL COMMENT '宠物id',
  `client_id` int NOT NULL COMMENT '客户id',
  `doctor_id` int NOT NULL COMMENT '开单医生id',
  `order_note` varchar(255) DEFAULT NULL COMMENT '检验目的/备注',
  `fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '化验费合计(元)',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0待检验 1已出结果',
  `result_note` varchar(255) DEFAULT NULL COMMENT '化验小结/结论',
  `result_by` varchar(64) DEFAULT NULL COMMENT '结果录入人',
  `create_time` datetime DEFAULT NULL COMMENT '开单时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`order_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='化验单主表';

-- ----------------------------
-- 3. 化验单明细
-- ----------------------------
DROP TABLE IF EXISTS `lab_order_item`;
CREATE TABLE `lab_order_item` (
  `item_id` int NOT NULL AUTO_INCREMENT COMMENT '明细id',
  `order_id` int NOT NULL COMMENT '化验单id',
  `test_id` int DEFAULT NULL COMMENT '字典检验项id',
  `item_name` varchar(64) NOT NULL COMMENT '冗余：项目名称',
  `item_group` varchar(32) DEFAULT NULL COMMENT '冗余：分组',
  `item_unit` varchar(32) DEFAULT NULL COMMENT '冗余：单位',
  `ref_range` varchar(64) DEFAULT NULL COMMENT '冗余：参考范围',
  `result_value` varchar(64) DEFAULT NULL COMMENT '检验结果值',
  `is_abnormal` tinyint(1) DEFAULT NULL COMMENT '是否异常',
  `result_time` datetime DEFAULT NULL COMMENT '出结果时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`item_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='化验单明细';

-- ----------------------------
-- 4. 账单表新增化验费字段
-- ----------------------------
ALTER TABLE `bill`
  ADD COLUMN `fee_lab` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '化验费(元)' AFTER `fee_drug`;

-- ----------------------------
-- 5. 预置常用检验项
-- ----------------------------
INSERT INTO `lab_test` (`test_name`, `test_group`, `test_unit`, `ref_range`, `test_price`, `active`) VALUES
('白细胞计数(WBC)', '血常规', '10^9/L', '6.0-17.0', 15.00, 1),
('红细胞计数(RBC)', '血常规', '10^12/L', '5.5-8.5', 15.00, 1),
('血红蛋白(HGB)', '血常规', 'g/L', '120-180', 15.00, 1),
('血小板计数(PLT)', '血常规', '10^9/L', '200-500', 15.00, 1),
('谷丙转氨酶(ALT)', '生化', 'U/L', '10-100', 40.00, 1),
('肌酐(CREA)', '生化', 'µmol/L', '44-159', 40.00, 1),
('尿素氮(BUN)', '生化', 'mmol/L', '2.5-9.6', 40.00, 1),
('尿比重(SG)', '尿检', '', '1.015-1.050', 10.00, 1),
('尿蛋白(Pro)', '尿检', '', '阴性', 10.00, 1),
('粪便镜检(虫卵)', '镜检', '', '未见', 8.00, 1);