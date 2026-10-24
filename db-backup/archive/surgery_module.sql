-- ============================================================
-- 手术/麻醉模块
-- 适用：MySQL 8 / MariaDB
-- 说明：手术排程 + 术中/术后记录 + 独立麻醉记录；手术费计入 bill.fee_surgery
-- ============================================================
SET NAMES utf8mb4;

-- 1. 手术单
DROP TABLE IF EXISTS `surgery`;
CREATE TABLE `surgery` (
  `surgery_id` int NOT NULL AUTO_INCREMENT COMMENT '手术单id',
  `record_id` int NOT NULL COMMENT '病历id',
  `pet_id` int NOT NULL COMMENT '宠物id',
  `client_id` int NOT NULL COMMENT '客户id',
  `doctor_id` int DEFAULT NULL COMMENT '主刀医生id',
  `surgery_name` varchar(128) NOT NULL COMMENT '手术名称',
  `surgery_type` varchar(32) DEFAULT NULL COMMENT '类型：绝育/骨科/肿瘤/外伤/其他',
  `anesthesia_type` varchar(32) DEFAULT NULL COMMENT '麻醉方式：全身麻醉/局部麻醉/镇静',
  `scheduled_time` datetime DEFAULT NULL COMMENT '预约手术时间',
  `start_time` datetime DEFAULT NULL COMMENT '实际开始',
  `end_time` datetime DEFAULT NULL COMMENT '实际结束',
  `surgeon` varchar(64) DEFAULT NULL COMMENT '主刀',
  `assistant` varchar(64) DEFAULT NULL COMMENT '助手',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '0待手术 1手术中 2已完成 3已取消',
  `preop_note` varchar(500) DEFAULT NULL COMMENT '术前评估/准备',
  `intraop_note` varchar(1000) DEFAULT NULL COMMENT '术中记录',
  `postop_note` varchar(1000) DEFAULT NULL COMMENT '术后记录/医嘱',
  `fee` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '手术费(元)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`surgery_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='手术单';

-- 2. 麻醉记录（一手术一条）
DROP TABLE IF EXISTS `anesthesia_record`;
CREATE TABLE `anesthesia_record` (
  `anesthesia_id` int NOT NULL AUTO_INCREMENT COMMENT '麻醉记录id',
  `surgery_id` int NOT NULL COMMENT '手术单id',
  `anesthesia_type` varchar(32) DEFAULT NULL COMMENT '麻醉方式',
  `anesthetist` varchar(64) DEFAULT NULL COMMENT '麻醉师',
  `start_time` datetime DEFAULT NULL COMMENT '麻醉开始',
  `end_time` datetime DEFAULT NULL COMMENT '麻醉结束',
  `drug_used` varchar(255) DEFAULT NULL COMMENT '麻醉用药',
  `vitals` varchar(500) DEFAULT NULL COMMENT '生命体征监护记录',
  `note` varchar(500) DEFAULT NULL COMMENT '备注/异常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`anesthesia_id`),
  KEY `idx_surgery` (`surgery_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='麻醉记录';

-- 3. 账单增加手术费分项
ALTER TABLE `bill`
  ADD COLUMN `fee_surgery` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '手术费(元)' AFTER `fee_package`;
