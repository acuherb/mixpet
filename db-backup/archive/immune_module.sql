-- ============================================================
-- 免疫/疫苗模块 新建表
-- 适用：MySQL 8 / MariaDB
-- 说明：疫苗字典 + 宠物接种档案；按疫苗建议间隔自动算下一针时间做到期提醒
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 疫苗字典
-- ----------------------------
DROP TABLE IF EXISTS `vaccine`;
CREATE TABLE `vaccine` (
  `vaccine_id` int NOT NULL AUTO_INCREMENT COMMENT '疫苗id',
  `vaccine_name` varchar(64) NOT NULL COMMENT '疫苗名称',
  `brand` varchar(64) DEFAULT NULL COMMENT '品牌/厂家',
  `prevent` varchar(128) DEFAULT NULL COMMENT '预防疾病',
  `interval_days` int NOT NULL DEFAULT '365' COMMENT '建议接种间隔(天)，用于推算下一针',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '参考单价(元)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`vaccine_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='疫苗字典';

-- ----------------------------
-- 宠物接种档案
-- ----------------------------
DROP TABLE IF EXISTS `immune_record`;
CREATE TABLE `immune_record` (
  `immune_id` int NOT NULL AUTO_INCREMENT COMMENT '接种记录id',
  `record_id` int DEFAULT NULL COMMENT '病历id(可空)',
  `pet_id` int NOT NULL COMMENT '宠物id',
  `client_id` int NOT NULL COMMENT '客户id',
  `doctor_id` int DEFAULT NULL COMMENT '接种医生id(可空)',
  `vaccine_id` int NOT NULL COMMENT '疫苗id',
  `vaccine_name` varchar(64) DEFAULT NULL COMMENT '冗余疫苗名',
  `dose_cnt` int NOT NULL DEFAULT '1' COMMENT '此针为第几针',
  `vaccinate_time` datetime NOT NULL COMMENT '本次接种时间',
  `next_time` datetime DEFAULT NULL COMMENT '下一针建议时间',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`immune_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_vaccine` (`vaccine_id`),
  KEY `idx_next` (`next_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='宠物接种档案';

-- ----------------------------
-- 预置常用疫苗
-- ----------------------------
INSERT INTO `vaccine` (`vaccine_name`, `brand`, `prevent`, `interval_days`, `price`) VALUES
('狂犬疫苗', '默沙东', '狂犬病', 365, 80.00),
('猫三联疫苗', '硕腾妙三多', '猫瘟/猫鼻支/猫杯状', 365, 120.00),
('犬四联疫苗', '英特威', '犬瘟/细小/副流感/腺病毒', 365, 100.00),
('犬八联疫苗', '辉瑞', '多种犬传染病', 365, 130.00),
('猫鼻支疫苗', '英特威', '猫鼻支', 365, 90.00),
('犬窝咳疫苗', '拜耳', '犬副流感', 365, 110.00);