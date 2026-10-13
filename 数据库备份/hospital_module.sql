-- ============================================================
-- 住院管理模块 新建表
-- 适用：MySQL 8 / MariaDB
-- 说明：出院时按天数×日均费计入 bill.fee_hospital，走现有收费列表
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 住院记录
-- ----------------------------
DROP TABLE IF EXISTS `hospital_record`;
CREATE TABLE `hospital_record` (
  `hospital_id` int NOT NULL AUTO_INCREMENT COMMENT '住院记录id',
  `record_id` int NOT NULL COMMENT '病历id',
  `pet_id` int NOT NULL COMMENT '宠物id',
  `client_id` int NOT NULL COMMENT '客户id',
  `doctor_id` int NOT NULL COMMENT '主治医生id',
  `room_no` varchar(32) DEFAULT NULL COMMENT '病房/床位号',
  `in_time` datetime DEFAULT NULL COMMENT '入院时间',
  `out_time` datetime DEFAULT NULL COMMENT '出院时间',
  `daily_fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '日均住院费(元)',
  `days` int NOT NULL DEFAULT '0' COMMENT '住院天数',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0住院中 1已出院',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`hospital_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='住院记录';

-- ----------------------------
-- 账单表新增住院费字段
-- ----------------------------
ALTER TABLE `bill`
  ADD COLUMN `fee_hospital` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '住院费(元)' AFTER `fee_lab`;