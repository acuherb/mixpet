/*M!999999\- enable the sandbox mode */ 
-- MariaDB dump 10.19-11.8.6-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: 127.0.0.1    Database: pet_mix
-- ------------------------------------------------------
-- Server version	11.8.6-MariaDB-0+deb13u1 from Debian

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*M!100616 SET @OLD_NOTE_VERBOSITY=@@NOTE_VERBOSITY, NOTE_VERBOSITY=0 */;

--
-- Table structure for table `adopt`
--

DROP TABLE IF EXISTS `adopt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `adopt` (
  `adopt_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '领养表的id',
  `pet_id` int(11) NOT NULL COMMENT '领养宠物的id',
  `client_id` int(11) NOT NULL COMMENT '领养人id',
  `adopt_money` int(11) NOT NULL DEFAULT 0 COMMENT '领养押金',
  `adopt_info` varchar(64) DEFAULT NULL COMMENT '订单备注',
  `in_adopt` tinyint(4) NOT NULL DEFAULT 2 COMMENT '是否通过，2为待审核，1为通过，0为未通过',
  `refuse_reason` varchar(255) DEFAULT NULL COMMENT '拒绝理由',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间，也是领养时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  PRIMARY KEY (`adopt_id`),
  KEY `pet_id` (`pet_id`),
  KEY `client_id` (`client_id`),
  CONSTRAINT `adopt_ibfk_1` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`pet_id`),
  CONSTRAINT `adopt_ibfk_2` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='领养宠物订单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `adopt`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `adopt` WRITE;
/*!40000 ALTER TABLE `adopt` DISABLE KEYS */;
INSERT INTO `adopt` VALUES
(1,26,1,234,'领养三毛',0,'111','2023-01-12 16:45:49','2023-02-12 21:58:52',0),
(2,34,3,300,'小狐狸^_^',0,'111','2023-01-12 21:38:33','2023-01-12 21:38:33',0),
(3,22,3,10,'五花比四花多一花',0,'test','2023-01-12 21:51:59','2023-01-12 21:51:59',0),
(4,19,1,888,'喵喵喵',0,NULL,'2023-01-19 12:12:01','2023-01-19 12:12:01',1791680038578),
(5,28,6,200,'养着玩',2,NULL,'2023-01-22 18:58:42','2023-01-22 18:58:42',0),
(6,23,3,32323,'测试',2,NULL,'2023-02-13 21:25:05','2023-02-13 21:25:05',0),
(7,19,2,2333,'狸二花，名花有主',2,NULL,'2023-02-13 21:39:15','2023-02-18 15:14:32',0),
(8,24,6,233,'金毛嘿嘿嘿',2,NULL,'2023-02-18 14:17:15','2023-02-18 14:17:15',0),
(9,14,1,300,'ces',2,NULL,'2023-08-23 22:38:08','2023-08-23 22:38:08',0),
(10,9,1,300,'家里条件很好，可以养',0,NULL,'2026-10-11 15:09:59','2026-10-11 15:10:40',1791705231473),
(11,9,1,300,'还想再试试',2,NULL,'2026-10-11 15:50:28','2026-10-11 15:50:28',1792312932619),
(12,3,14,100,'测试入栏的猫',2,NULL,'2026-10-11 19:46:15','2026-10-11 19:46:15',1792312925622),
(13,3,1,300,'老子有实力',1,NULL,'2026-10-11 20:49:28','2026-10-11 20:49:28',0),
(14,9,14,300,'试试看',1,NULL,'2026-10-15 13:14:02','2026-10-15 13:14:02',0);
/*!40000 ALTER TABLE `adopt` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `ai_advice`
--

DROP TABLE IF EXISTS `ai_advice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_advice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `record_id` bigint(20) DEFAULT NULL COMMENT 'å…³è”ç—…åŽ†',
  `provider` varchar(32) NOT NULL DEFAULT 'mock' COMMENT 'æ¨¡åž‹æä¾›æ–¹ mock/deepseek',
  `request_hash` varchar(64) DEFAULT NULL COMMENT 'å…¥å‚æ‘˜è¦hash,ç”¨äºŽé˜²é‡å¤',
  `advice_json` text DEFAULT NULL COMMENT 'ç»“æž„åŒ–å»ºè®®JSON',
  `doctor` bigint(20) DEFAULT NULL COMMENT 'è§¦å‘åŒ»ç”Ÿ',
  `create_time` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'ç”Ÿæˆæ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='AIè¾…åŠ©è¯Šç–—ç•™ç—•';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ai_advice`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `ai_advice` WRITE;
/*!40000 ALTER TABLE `ai_advice` DISABLE KEYS */;
INSERT INTO `ai_advice` VALUES
(1,18,'mock','7f1627b7e6cbaf8be55264a1ecebbc99','{\"differentials\":[{\"cond\":\"需补充问诊信息\",\"prob\":\"—\",\"basis\":\"当前主诉/现病史信息较少，完善后再获取更可靠的鉴别方向\"}],\"examSuggest\":[\"血常规 + 生化（基础评估）\",\"按体重 0.20kg 核算给药剂量与输液量\"],\"indexExplain\":[\"暂无检验异常项；录入检验结果后将在此做关联解读\",\"影像报告已出，建议与检验结果互为印证（如影像定位 + 化验定性）\"],\"treatRef\":[\"对症支持：按脱水程度补液，消化道病例短期禁食后少量多餐过渡\",\"按检验/影像结果选择用药，避免无指征使用抗生素\",\"记录 24 小时进食、排便、精神与体温变化，复诊时对比评估疗效\"],\"ownerNote\":\"您好，波二斯目前主要表现为皮肤病。我们已为它安排相应检查，初步方向已记录在病历医嘱中，接下来会按方案治疗并观察反应。治疗期间如出现精神明显变差、症状加重或拒食等情况，请随时联系我们。\",\"revisitSuggest\":\"建议 3-5 天内复诊评估疗效；若症状加重或出现新情况，请提前就诊。\"}',10,'2026-10-20 15:58:25'),
(2,19,'mock','b92f6f02eaa5aafc12e012009b4b51d8','{\"differentials\":[{\"cond\":\"外伤/软组织损伤\",\"prob\":\"高\",\"basis\":\"突发跛行先排除外伤，结合触诊\"},{\"cond\":\"骨科疾病（髌骨/髋关节/韧带）\",\"prob\":\"中\",\"basis\":\"小型犬髌骨脱位、大型犬十字韧带损伤常见\"}],\"examSuggest\":[\"患肢 X 线（关节/骨骼）\",\"按体重 0.70kg 核算给药剂量与输液量\"],\"indexExplain\":[\"暂无检验异常项；录入检验结果后将在此做关联解读\"],\"treatRef\":[\"对症支持：按脱水程度补液，消化道病例短期禁食后少量多餐过渡\",\"按检验/影像结果选择用药，避免无指征使用抗生素\",\"记录 24 小时进食、排便、精神与体温变化，复诊时对比评估疗效\"],\"ownerNote\":\"您好，小三儿目前主要表现为打喷嚏。我们已为它安排相应检查，初步方向已记录在病历医嘱中，接下来会按方案治疗并观察反应。治疗期间如出现精神明显变差、症状加重或拒食等情况，请随时联系我们。\",\"revisitSuggest\":\"建议 3-5 天内复诊评估疗效；若症状加重或出现新情况，请提前就诊。\"}',10,'2026-10-20 18:31:04');
/*!40000 ALTER TABLE `ai_advice` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `anesthesia_record`
--

DROP TABLE IF EXISTS `anesthesia_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `anesthesia_record` (
  `anesthesia_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '麻醉记录id',
  `surgery_id` int(11) NOT NULL COMMENT '手术单id',
  `anesthesia_type` varchar(32) DEFAULT NULL COMMENT '麻醉方式',
  `anesthetist` varchar(64) DEFAULT NULL COMMENT '麻醉师',
  `start_time` datetime DEFAULT NULL COMMENT '麻醉开始',
  `end_time` datetime DEFAULT NULL COMMENT '麻醉结束',
  `drug_used` varchar(255) DEFAULT NULL COMMENT '麻醉用药',
  `vitals` varchar(500) DEFAULT NULL COMMENT '生命体征监护记录',
  `note` varchar(500) DEFAULT NULL COMMENT '备注/异常',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`anesthesia_id`),
  KEY `idx_surgery` (`surgery_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='麻醉记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `anesthesia_record`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `anesthesia_record` WRITE;
/*!40000 ALTER TABLE `anesthesia_record` DISABLE KEYS */;
INSERT INTO `anesthesia_record` VALUES
(1,1,'','',NULL,NULL,'','','','2026-10-13 21:06:35','2026-10-20 20:37:41',0);
/*!40000 ALTER TABLE `anesthesia_record` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `appointment`
--

DROP TABLE IF EXISTS `appointment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `appointment` (
  `appointment_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '挂号单id',
  `client_id` int(11) NOT NULL COMMENT '用户id',
  `pet_id` int(11) NOT NULL COMMENT '宠物id',
  `pet_owner_id` int(11) NOT NULL COMMENT '就诊时的宠物主人id',
  `appointment_date` datetime NOT NULL COMMENT '就诊时间',
  `department_id` int(11) NOT NULL COMMENT '部门id',
  `doctor_id` int(11) NOT NULL COMMENT '医生id',
  `appointment_info` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '挂号简短信息',
  `is_suc` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否处理',
  `miss` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0正常 1爽约(医生标记，记录保留不删)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  `origin_record_id` int(11) DEFAULT NULL COMMENT 'å…³è”çš„åˆè¯Šç—…åŽ†id(å¤è¯ŠæŒ‚å·æ—¶å¡«)',
  PRIMARY KEY (`appointment_id`),
  KEY `fk_appointment_client` (`client_id`),
  KEY `fk_appointment_pet` (`pet_id`),
  KEY `fk_appointment_department` (`department_id`),
  KEY `fk_appointment_doctor` (`doctor_id`),
  CONSTRAINT `fk_appointment_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`),
  CONSTRAINT `fk_appointment_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`),
  CONSTRAINT `fk_appointment_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`doctor_id`),
  CONSTRAINT `fk_appointment_pet` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`pet_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='挂号单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `appointment`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `appointment` WRITE;
/*!40000 ALTER TABLE `appointment` DISABLE KEYS */;
INSERT INTO `appointment` VALUES
(1,1,5,1,'2022-12-22 11:17:54',1,1,'严重的肛裂',1,0,'2022-12-22 11:18:04','2023-03-03 15:51:32',0,NULL),
(2,4,5,4,'2023-01-08 21:03:00',4,5,'尝试第一次挂号',0,0,'2023-01-08 21:04:35','2023-01-09 13:26:39',0,NULL),
(3,3,12,3,'2023-01-08 23:30:00',8,4,'熬夜写网页',1,0,'2023-01-08 23:31:23','2023-03-08 18:22:09',0,NULL),
(4,1,3,1,'2023-01-16 21:24:00',8,4,'查看骨骼',1,0,'2023-01-16 21:24:51','2023-03-08 18:21:37',0,NULL),
(5,3,12,3,'2023-01-17 20:37:00',4,5,'测试挂号',0,0,'2023-01-17 20:37:12','2023-01-21 15:41:44',0,NULL),
(6,3,12,3,'2023-02-13 23:49:00',8,4,'熬夜写网页',1,0,'2023-02-13 21:49:47','2023-03-08 18:21:58',0,NULL),
(7,3,12,3,'2023-02-18 14:30:00',8,4,'测试挂号',0,0,'2023-02-18 14:30:21','2023-03-08 18:21:48',0,NULL),
(8,4,5,4,'2026-10-10 13:35:00',1,1,'测试',1,0,'2026-10-10 13:35:26','2026-10-10 13:35:26',0,NULL),
(9,1,14,1,'2026-10-11 09:12:00',1,1,'拉稀',0,0,'2026-10-11 09:12:59','2026-10-11 09:12:59',0,NULL),
(10,1,39,1,'2026-10-12 00:00:00',8,4,'精神不佳',1,0,'2026-10-12 11:29:38','2026-10-12 11:29:38',0,NULL),
(11,4,5,4,'2026-10-12 20:36:00',11,10,'只是看盾医生而已',1,0,'2026-10-12 20:37:36','2026-10-12 20:37:36',0,NULL),
(12,1,37,1,'2026-10-13 11:04:00',8,10,'咳嗽',1,0,'2026-10-13 11:04:50','2026-10-13 11:04:50',0,NULL),
(13,14,35,14,'2026-10-14 00:00:00',2,10,'左前肢无力',1,0,'2026-10-13 20:55:51','2026-10-13 20:55:51',0,NULL),
(14,14,35,14,'2026-10-14 00:00:00',8,10,'拉稀',1,0,'2026-10-13 21:00:02','2026-10-13 21:00:02',0,NULL),
(15,14,35,14,'2026-10-15 15:54:00',1,1,'咳嗽',0,0,'2026-10-15 15:55:00','2026-10-15 15:55:00',0,NULL),
(16,6,8,6,'2026-10-16 19:18:00',11,10,'皮肤病',1,0,'2026-10-16 19:18:44','2026-10-16 19:18:44',0,NULL),
(17,1,36,1,'2026-10-17 14:17:00',8,10,'打喷嚏',1,0,'2026-10-17 14:17:37','2026-10-18 18:16:48',0,NULL),
(18,14,35,14,'2026-10-22 17:55:00',8,10,'1323',0,0,'2026-10-22 17:56:00','2026-10-22 17:56:00',1792662979262,NULL),
(19,14,35,14,'2026-10-22 17:56:00',8,10,'123',0,0,'2026-10-22 17:56:58','2026-10-22 17:56:58',0,NULL);
/*!40000 ALTER TABLE `appointment` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `bill`
--

DROP TABLE IF EXISTS `bill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `bill` (
  `bill_id` int(11) NOT NULL AUTO_INCREMENT,
  `client_id` int(11) NOT NULL,
  `pet_id` int(11) NOT NULL,
  `record_id` int(11) DEFAULT NULL,
  `fee_register` decimal(10,2) DEFAULT 0.00 COMMENT '挂号费',
  `fee_treat` decimal(10,2) DEFAULT 0.00 COMMENT '诊疗费',
  `fee_drug` decimal(10,2) DEFAULT 0.00 COMMENT '药费',
  `fee_lab` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '化验费(元)',
  `fee_hospital` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '住院费(元)',
  `fee_image` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '影像费(元)',
  `fee_package` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '套餐费(元)',
  `fee_surgery` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '手术费(元)',
  `fee_immune` decimal(10,2) DEFAULT 0.00 COMMENT 'ç–«è‹—è´¹ï¼ˆå…ç–«æŽ¥ç§è®¡å…¥ï¼‰',
  `total_money` decimal(10,2) DEFAULT 0.00,
  `pay_status` tinyint(1) DEFAULT 0 COMMENT '0未付1已付',
  `pay_type` tinyint(1) DEFAULT 0 COMMENT '0余额1现金',
  `pay_by` varchar(50) DEFAULT NULL COMMENT 'æ”¶æ¬¾æ“ä½œäººå§“åï¼ˆè°ç™»å½•è°æ”¶æ¬¾å³è®°å½•ï¼‰',
  `is_del` bigint(20) DEFAULT 0,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `dispense_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'å‘è¯çŠ¶æ€ 0æœªå‘ 1å·²å‘',
  `dispense_by` varchar(50) DEFAULT NULL COMMENT 'å‘è¯æ ¸å¯¹äºº',
  `dispense_time` datetime DEFAULT NULL COMMENT 'å‘è¯æ ¸å¯¹æ—¶é—´',
  `refund_status` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'é€€æ¬¾çŠ¶æ€ 0æœªé€€ 1å·²é€€',
  `refund_by` varchar(50) DEFAULT NULL COMMENT 'é€€æ¬¾æ“ä½œäºº',
  `refund_reason` varchar(255) DEFAULT NULL COMMENT '退款原因（退款时必填）',
  `refund_time` datetime DEFAULT NULL COMMENT 'é€€æ¬¾æ—¶é—´',
  `discount` decimal(3,2) DEFAULT NULL COMMENT 'ä¼šå‘˜æŠ˜æ‰£çŽ‡å¿«ç…§(1=ä¸æ‰“æŠ˜)',
  `discount_money` decimal(12,2) GENERATED ALWAYS AS (round(`total_money` * (1 - ifnull(`discount`,1)),2)) STORED COMMENT 'ä¼˜æƒ é‡‘é¢',
  `real_money` decimal(12,2) GENERATED ALWAYS AS (round(`total_money` * ifnull(`discount`,1),2)) STORED COMMENT 'æŠ˜åŽåº”ä»˜/å®žæ”¶',
  PRIMARY KEY (`bill_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_record_pay` (`record_id`,`pay_status`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='收费账单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bill`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `bill` WRITE;
/*!40000 ALTER TABLE `bill` DISABLE KEYS */;
INSERT INTO `bill` VALUES
(1,1,39,1,5.00,20.00,67.50,0.00,0.00,0.00,0.00,0.00,0.00,92.50,1,1,NULL,0,'2026-10-12 11:33:46','2026-10-15 11:59:17',0,NULL,NULL,0,NULL,NULL,NULL,1.00,0.00,92.50),
(2,1,37,12,10.00,0.00,0.00,0.00,0.00,0.00,0.00,2000.00,0.00,2010.00,1,1,NULL,0,'2026-10-13 19:48:00','2026-10-15 12:37:32',0,NULL,NULL,0,NULL,NULL,NULL,1.00,0.00,2010.00),
(3,4,5,15,10.00,20.00,94.00,0.00,0.00,200.00,299.00,0.00,0.00,623.00,1,1,NULL,0,'2026-10-13 20:04:27','2026-10-15 21:50:11',0,NULL,NULL,0,NULL,NULL,NULL,1.00,0.00,623.00),
(4,14,35,16,10.00,0.00,26.00,0.00,0.00,200.00,0.00,0.00,0.00,236.00,1,1,NULL,0,'2026-10-14 11:23:11','2026-10-16 19:19:04',0,NULL,NULL,0,NULL,NULL,NULL,1.00,0.00,236.00),
(5,14,35,17,10.00,0.00,15.50,0.00,0.00,100.00,0.00,0.00,0.00,125.50,0,0,NULL,0,'2026-10-14 16:56:48','2026-10-15 11:59:17',0,NULL,NULL,0,NULL,NULL,NULL,1.00,0.00,125.50),
(8,6,8,18,10.00,0.00,54.50,0.00,0.00,250.00,99.00,0.00,0.00,413.50,0,0,NULL,0,'2026-10-17 11:16:56','2026-10-18 19:06:10',0,NULL,NULL,0,NULL,NULL,NULL,1.00,0.00,413.50),
(9,1,36,19,10.00,0.00,65.00,55.00,0.00,400.00,99.00,666.00,80.00,1375.00,1,1,'管理员',0,'2026-10-20 15:51:08','2026-10-22 15:52:56',0,NULL,NULL,0,NULL,NULL,NULL,1.00,0.00,1375.00);
/*!40000 ALTER TABLE `bill` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `client`
--

DROP TABLE IF EXISTS `client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `client` (
  `client_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `client_username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '用户账号',
  `client_password` varchar(72) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT '$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye' COMMENT '用户密码',
  `client_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户姓名',
  `client_gender` tinyint(1) DEFAULT 0 COMMENT '用户性别',
  `client_tel` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户电话',
  `client_birthday` date DEFAULT NULL COMMENT '用户生日',
  `client_birthday_estimated` tinyint(1) NOT NULL DEFAULT 0 COMMENT '生日是否估算 0精确 1估算',
  `client_info` varchar(255) DEFAULT '一个新用户' COMMENT '用户简介',
  `client_photo` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'zs.jpg' COMMENT '用户照片名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 0停用 1正常',
  `audit_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'å®¡æ ¸çŠ¶æ€ï¼š0å¾…å®¡æ ¸ 1é€šè¿‡ 2æ‹’ç»',
  `audit_reason` varchar(255) DEFAULT NULL COMMENT 'æ³¨å†Œå®¡æ ¸æ‹’ç»åŽŸå› (ç™»å½•æ—¶æç¤ºé¡¾å®¢)',
  `client_tel2` varchar(32) DEFAULT NULL COMMENT '备用电话',
  `client_email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `client_pwd_changed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否改过密码 0未改 1已改',
  `client_address` varchar(128) DEFAULT NULL COMMENT '地址',
  `client_id_card` varchar(18) DEFAULT NULL COMMENT '身份证',
  `client_level` tinyint(4) NOT NULL DEFAULT 0 COMMENT '会员等级 0普通 1银 2金 3钻石',
  `client_points` int(11) NOT NULL DEFAULT 0 COMMENT '积分',
  `client_balance` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
  `client_source` varchar(16) DEFAULT NULL COMMENT '客户来源',
  `client_blacklist` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否黑名单 0正常 1拉黑',
  PRIMARY KEY (`client_id`),
  UNIQUE KEY `client_username` (`client_username`,`is_del`) USING BTREE,
  KEY `idx_tel` (`client_tel`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `client` WRITE;
/*!40000 ALTER TABLE `client` DISABLE KEYS */;
INSERT INTO `client` VALUES
(1,'13333333336','$2a$10$IIHPurrTgCXnxlIem5JTre83r5fJkzaJ2XLYJlgY/w17qsHC6w2Ty','莹',0,'13333333336','2000-10-10',1,'我是一个问题人儿，随时提问题来修改逻辑','01d364c5-4ece-439d-9dee-76ff781347aa.webp','2022-12-21 19:24:32','2026-10-16 15:40:53',0,1,1,NULL,'123456',NULL,1,'地球村',NULL,0,3385,0.00,NULL,0),
(2,'13222222222','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','雷军',0,'13222222222','1989-08-10',1,'喜欢宅家撸猫','lei-jun.jpg','2023-01-01 20:03:13','2026-10-10 18:15:24',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(3,'10000000001','$2a$10$6Br1LBrKlZTNlnon7MzD5ucm7ERcXfzMq7CnUOFWFbvxRcb9C8FI6','散玉',1,'10000000001','2000-01-01',0,'作者','san-yu.jpg','2023-01-01 21:07:33','2023-01-09 13:20:26',0,1,1,NULL,NULL,NULL,1,NULL,NULL,0,0,0.00,NULL,0),
(4,'10000000002','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','狐狸',0,'10000000002','2000-01-07',0,'粉毛狐狸','ba-chong.jpg','2023-01-07 16:01:10','2023-01-08 16:13:26',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,623,0.00,NULL,0),
(5,'10000000003','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','红中',1,'10000000003','2023-01-22',0,'一个新用户','zs.jpg','2023-01-22 14:44:24','2023-03-02 20:35:10',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(6,'10000000004','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','北风',0,'10000000004','2000-01-03',0,'黜聪明，拙肢体，同于大道','zs.jpg','2023-01-22 14:54:49','2023-01-22 22:55:31',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(7,'10000000005','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','骰子',1,'10000000005','2023-01-22',0,'一个新用户','zs.jpg','2023-01-22 15:23:53','2023-01-22 15:23:53',1690468063801,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(9,'10000000006','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','骰子',1,'10000000006','2023-01-22',0,'一个新用户','zs.jpg','2023-01-22 15:30:02','2023-03-02 20:35:01',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(10,'10000000007','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','六饼',0,'10000000007','2023-01-22',0,'一个新用户','zs.jpg','2023-01-22 15:38:26','2023-01-22 15:38:26',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(11,'10000000008','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','五饼',1,'10000000008','2023-01-23',0,'一个新用户','zs.jpg','2023-01-23 10:28:24','2023-01-23 10:28:24',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(12,'10000000009','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','六条',1,'10000000009','2023-01-23',0,'666666','zs.jpg','2023-01-23 11:00:28','2023-01-23 11:00:41',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(13,'10000000010','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','八条',1,'10000000010','2023-02-09',0,'一个新用户','zs.jpg','2023-02-09 15:27:34','2023-02-09 15:27:34',0,1,1,NULL,NULL,NULL,0,NULL,NULL,0,0,0.00,NULL,0),
(14,'13333333331','$2a$10$7ALBsCShLsbAPJt57/m0Ne5U5st/8PGUp/zgRnu2nj4QezvfrXime','张三',1,'13333333331','2017-10-04',0,'人挺 好的，朋友级别','631ce29c-6881-47ab-82fd-ecabec7e25aa.webp','2026-10-10 17:53:50','2026-10-15 21:51:55',0,1,1,NULL,NULL,NULL,1,NULL,NULL,0,236,0.00,NULL,0),
(18,'13312311234','$2a$10$psNucMnp/PznvY.rF7xqWu7c3iiD/1WIjDRRb6izZ2ALd39S8uQmS','王忱',1,'13312311234',NULL,0,'一个新用户','zs.jpg','2026-10-22 19:19:47','2026-10-23 17:24:57',0,1,1,NULL,'','124768092@qq.com',1,'','',0,0,0.00,NULL,0);
/*!40000 ALTER TABLE `client` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `client_count_card`
--

DROP TABLE IF EXISTS `client_count_card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `client_count_card` (
  `card_id` int(11) NOT NULL AUTO_INCREMENT,
  `client_id` int(11) NOT NULL,
  `template_id` int(11) DEFAULT NULL,
  `name` varchar(128) DEFAULT NULL,
  `remain_times` int(11) NOT NULL,
  `total_times` int(11) NOT NULL,
  `expire_date` date DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1æœ‰æ•ˆ 0ç”¨å®Œ/ä½œåºŸ',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`card_id`),
  KEY `idx_client` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='å®¢æˆ·æ¬¡å¡';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `client_count_card`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `client_count_card` WRITE;
/*!40000 ALTER TABLE `client_count_card` DISABLE KEYS */;
/*!40000 ALTER TABLE `client_count_card` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `count_card_template`
--

DROP TABLE IF EXISTS `count_card_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `count_card_template` (
  `template_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `service_type` varchar(64) DEFAULT NULL,
  `times` int(11) NOT NULL COMMENT 'æ¬¡æ•°',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00,
  `validity_days` int(11) DEFAULT NULL COMMENT 'æœ‰æ•ˆå¤©æ•°',
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_del` bigint(20) DEFAULT 0,
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='æ¬¡å¡æ¨¡æ¿';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `count_card_template`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `count_card_template` WRITE;
/*!40000 ALTER TABLE `count_card_template` DISABLE KEYS */;
INSERT INTO `count_card_template` VALUES
(1,'洗澡卡','洗护',10,500.00,365,1,'2026-10-16 19:14:32','2026-10-16 19:14:32',0);
/*!40000 ALTER TABLE `count_card_template` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `department_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '科室id',
  `department_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '科室名称',
  `department_info` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '科室简介',
  `head_id` int(11) DEFAULT NULL COMMENT '主任ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  PRIMARY KEY (`department_id`),
  UNIQUE KEY `department_name` (`department_name`),
  KEY `head_id` (`head_id`),
  CONSTRAINT `department_ibfk_1` FOREIGN KEY (`head_id`) REFERENCES `doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES
(1,'内科','犬猫综合内科、常见病与传染病',2,'2022-12-22 11:11:19','2026-10-12 18:03:24',0),
(2,'骨科','骨折、关节与脊椎疾病',8,'2023-01-03 21:00:02','2023-08-23 23:44:15',0),
(3,'化验室','血常规、生化、镜检等检验',4,'2023-01-03 21:02:17','2026-10-16 19:35:03',0),
(4,'美容洗护室','宠物美容、洗澡与日常护理',9,'2023-01-03 21:04:50','2026-10-12 18:01:36',0),
(8,'中兽医科','中兽医、针灸与中药调理',8,'2023-01-07 20:11:51','2026-10-12 18:01:45',0),
(9,'外科','外科手术与创伤处理',1,'2026-10-12 14:31:23','2026-10-12 18:03:42',0),
(10,'影像科','X光、B超、内窥镜影像诊断',6,'2026-10-12 14:31:23','2026-10-17 19:38:34',0),
(11,'皮肤科','皮肤病与皮肤过敏诊治',9,'2026-10-12 14:31:23','2026-10-17 19:38:44',0),
(12,'眼科与口腔科','眼科、耳鼻喉与口腔疾病',10,'2026-10-12 14:31:23','2026-10-17 19:38:55',0);
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `doctor`
--

DROP TABLE IF EXISTS `doctor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor` (
  `doctor_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '医生id',
  `department_id` int(11) DEFAULT NULL COMMENT '部门id',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '医生帐号',
  `doctor_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '医生姓名',
  `doctor_gender` tinyint(1) NOT NULL DEFAULT 0 COMMENT '医生性别，1男0女',
  `doctor_age` date NOT NULL DEFAULT '2001-01-03' COMMENT '医生生日',
  `doctor_photo` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'defaultDoctor.jpg' COMMENT '医生照片名称',
  `doctor_tel` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '医生联系方式',
  `doctor_email` varchar(64) DEFAULT NULL COMMENT '医生邮箱',
  `auth_lv` int(11) DEFAULT 0 COMMENT '权限等级',
  `job_id` int(11) DEFAULT NULL COMMENT '职称id(关联job)',
  `doctor_info` varchar(1000) NOT NULL DEFAULT '',
  `doctor_cert` varchar(64) DEFAULT NULL COMMENT '执业兽医资格证号(处方笺用)',
  `doctor_motto` varchar(255) NOT NULL DEFAULT '' COMMENT '医生座右铭',
  `doctor_password` varchar(72) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye' COMMENT '医生密码',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  PRIMARY KEY (`doctor_id`),
  UNIQUE KEY `doctor_code` (`username`) USING BTREE,
  KEY `fk_doctor_department` (`department_id`),
  CONSTRAINT `fk_doctor_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='医生表，外键部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `doctor` WRITE;
/*!40000 ALTER TABLE `doctor` DISABLE KEYS */;
INSERT INTO `doctor` VALUES
(1,1,'yun','童德统',1,'1990-10-10','doctor-1.jpg','13123456789','sanyu0201@163.com',4,3,'国内著名自封医学专家，擅长绝育（公的为主），现在定居倭国',NULL,'','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','2022-12-22 11:16:11','2026-10-14 18:04:32',0),
(2,4,'ais','爱丽丝',0,'2003-02-01','doctor-2.jpg','','sanyu0201@163.com',4,3,'擅长麻醉',NULL,'','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','2023-01-06 12:25:54','2026-10-13 15:47:00',0),
(4,8,'tom','汤姆',1,'2000-01-11','doctor-3.jpg','','sanyu0201@163.com',4,5,'擅长x光拍片',NULL,'','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','2023-01-06 12:29:10','2023-08-23 22:40:05',0),
(5,4,'zdz','张大嘴',0,'2002-01-05','doctor-4.jpg','','sanyu0201@163.com',4,5,'著名宠物医美专家',NULL,'','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','2023-01-06 12:30:19','2023-08-23 22:40:13',0),
(6,3,'ra9','李妙手',1,'2001-01-06','3b161abe-7142-449c-ab36-07a689da65de.jpg','','sanyu0201@163.com',2,9,'国外著名医学专家',NULL,'','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','2023-01-06 12:31:30','2023-08-23 22:40:26',0),
(8,2,'lzg','李正骨',0,'2000-01-28','3cd8e749-e177-4e5b-846c-930efab7bd38.jpg','','sanyu0201@163.com',4,5,'国内外著名骨科专家',NULL,'','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','2023-01-07 16:35:23','2023-08-23 22:40:32',0),
(9,8,'zhong','钟回春',1,'2000-02-10','92833192-79d4-42d6-a657-d000d8ca6295.jpg','','sanyu0201@163.com',4,5,'多年老中医',NULL,'','$2a$10$isx1czTSQaNBZiYiwo/TjuEb40VFhH6VqAXOsHfFK39ls.DxziPye','2023-02-18 14:08:38','2023-08-23 22:40:41',0),
(10,8,'admin','管理员',1,'2000-08-15','defaultDoctor.jpg','13223309123','sanyu0201@163.com',8,1,'凡大医治病，必当安神定志，无欲无求，先发大慈恻隐之心，誓愿普救含灵之苦。若有疾厄来求救者，不得问其贵贱贫富，长幼妍蚩，怨亲善友，华夷愚智，普同一等，皆如至亲之想。亦不得瞻前顾后，自虑吉凶，护惜身命。见彼苦恼，若己有之，深心凄怆，勿避险巇、昼夜、寒暑、饥渴、疲劳，一心赴救，无作功夫形迹之心。如此可为苍生大医，反此则是含灵巨贼。',NULL,'凡大医治病，必当安神定志，无欲无求，先发大慈恻隐之心，誓愿普救含灵之苦。','$2a$10$6Br1LBrKlZTNlnon7MzD5ucm7ERcXfzMq7CnUOFWFbvxRcb9C8FI6','2023-08-23 22:30:29','2026-10-19 16:12:51',0);
/*!40000 ALTER TABLE `doctor` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `doctor_department`
--

DROP TABLE IF EXISTS `doctor_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_department` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doctor_id` int(11) NOT NULL COMMENT '医生id',
  `department_id` int(11) NOT NULL COMMENT '科室id',
  `create_time` datetime DEFAULT current_timestamp() COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT 'ä¿®æ”¹æ—¶é—´',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_depart` (`doctor_id`,`department_id`)
) ENGINE=InnoDB AUTO_INCREMENT=159 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='医生可执业科室（多对多）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor_department`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `doctor_department` WRITE;
/*!40000 ALTER TABLE `doctor_department` DISABLE KEYS */;
INSERT INTO `doctor_department` VALUES
(2,8,2,'2026-10-12 17:37:58',NULL),
(3,6,3,'2026-10-12 17:37:58',NULL),
(5,5,4,'2026-10-12 17:37:58',NULL),
(6,4,8,'2026-10-12 17:37:58',NULL),
(7,9,8,'2026-10-12 17:37:58',NULL),
(136,2,11,'2026-10-13 15:47:00',NULL),
(137,2,10,'2026-10-13 15:47:00',NULL),
(138,2,12,'2026-10-13 15:47:00',NULL),
(139,2,9,'2026-10-13 15:47:00',NULL),
(140,2,8,'2026-10-13 15:47:00',NULL),
(141,1,1,'2026-10-14 18:04:32',NULL),
(142,1,2,'2026-10-14 18:04:32',NULL),
(143,1,3,'2026-10-14 18:04:32',NULL),
(144,1,4,'2026-10-14 18:04:32',NULL),
(145,1,8,'2026-10-14 18:04:32',NULL),
(146,1,9,'2026-10-14 18:04:32',NULL),
(147,1,10,'2026-10-14 18:04:32',NULL),
(148,1,11,'2026-10-14 18:04:32',NULL),
(149,1,12,'2026-10-14 18:04:32',NULL),
(150,10,1,'2026-10-19 16:12:51',NULL),
(151,10,2,'2026-10-19 16:12:51',NULL),
(152,10,3,'2026-10-19 16:12:51',NULL),
(153,10,4,'2026-10-19 16:12:51',NULL),
(154,10,8,'2026-10-19 16:12:51',NULL),
(155,10,9,'2026-10-19 16:12:51',NULL),
(156,10,10,'2026-10-19 16:12:51',NULL),
(157,10,11,'2026-10-19 16:12:51',NULL),
(158,10,12,'2026-10-19 16:12:51',NULL);
/*!40000 ALTER TABLE `doctor_department` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `doctor_schedule`
--

DROP TABLE IF EXISTS `doctor_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `doctor_schedule` (
  `schedule_id` int(11) NOT NULL AUTO_INCREMENT,
  `doctor_id` int(11) NOT NULL COMMENT 'åŒ»ç”Ÿid',
  `doctor_name` varchar(64) DEFAULT NULL COMMENT 'å†—ä½™åŒ»ç”Ÿå',
  `department_id` int(11) DEFAULT NULL,
  `weekday` int(11) NOT NULL COMMENT '1å‘¨ä¸€..7å‘¨æ—¥',
  `period` int(11) NOT NULL COMMENT '1ä¸Šåˆ 2ä¸‹åˆ',
  `max_count` int(11) NOT NULL DEFAULT 10 COMMENT 'å¯çº¦å·æº',
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_del` bigint(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (`schedule_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='åŒ»ç”ŸæŽ’ç­';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `doctor_schedule`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `doctor_schedule` WRITE;
/*!40000 ALTER TABLE `doctor_schedule` DISABLE KEYS */;
/*!40000 ALTER TABLE `doctor_schedule` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `drug`
--

DROP TABLE IF EXISTS `drug`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `drug` (
  `drug_id` int(11) NOT NULL AUTO_INCREMENT,
  `drug_name` varchar(64) NOT NULL COMMENT '药品名',
  `drug_spec` varchar(64) DEFAULT NULL COMMENT '规格,如0.25g×24片',
  `brand` varchar(64) DEFAULT NULL COMMENT '品牌/厂家(疫苗)',
  `prevent` varchar(128) DEFAULT NULL COMMENT '预防疾病(疫苗)',
  `interval_days` int(11) DEFAULT NULL COMMENT '建议接种间隔天数(疫苗，用于推算下一针)',
  `dose_per_kg` decimal(10,3) DEFAULT NULL COMMENT 'æ¯kgä½“é‡å•æ¬¡ç”¨é‡(å•ä½åŒè¯å‰‚å•ä½)',
  `dose_fixed` decimal(10,2) DEFAULT NULL COMMENT 'å›ºå®šå•æ¬¡ç”¨é‡(ä¸Žä½“é‡æ— å…³ï¼Œä¼˜å…ˆ)',
  `drug_unit` varchar(16) DEFAULT '盒' COMMENT '单位',
  `drug_type` varchar(16) DEFAULT '西药' COMMENT '分类',
  `drug_price` decimal(10,2) DEFAULT 0.00 COMMENT '单价',
  `drug_stock` int(11) DEFAULT 0 COMMENT '库存',
  `drug_info` varchar(255) DEFAULT NULL COMMENT '说明',
  `active` tinyint(1) DEFAULT 1,
  `is_del` bigint(20) DEFAULT 0,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `warning_stock` int(11) NOT NULL DEFAULT 0 COMMENT 'åº“å­˜é¢„è­¦é˜ˆå€¼',
  `expire_date` date DEFAULT NULL COMMENT 'æœ‰æ•ˆæœŸè‡³',
  `cost_price` decimal(10,2) DEFAULT NULL COMMENT 'è¿›è´§ä»·(æˆæœ¬ä»·)ï¼Œç”¨äºŽè¯å“æ¯›åˆ©æ ¸ç®—',
  PRIMARY KEY (`drug_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='药品字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drug`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `drug` WRITE;
/*!40000 ALTER TABLE `drug` DISABLE KEYS */;
INSERT INTO `drug` VALUES
(1,'阿莫西林克拉维酸钾片','625mg×7片',NULL,NULL,NULL,NULL,NULL,'盒','西药',28.00,50,'广谱抗生素，遵医嘱使用',1,0,'2026-10-12 09:07:51','2026-10-12 09:07:51',0,NULL,NULL),
(2,'头孢氨苄片','0.25g×24片',NULL,NULL,NULL,NULL,NULL,'盒','西药',15.50,59,'头孢类抗生素',1,0,'2026-10-12 09:07:51','2026-10-12 12:33:53',0,NULL,NULL),
(3,'甲硝唑片','0.2g×21片',NULL,NULL,NULL,NULL,NULL,'盒','西药',8.00,80,'抗厌氧菌',1,0,'2026-10-12 09:07:51','2026-10-12 09:07:51',0,NULL,NULL),
(4,'宠物驱虫片(犬)','10kg规格',NULL,NULL,NULL,NULL,NULL,'盒','驱虫',45.00,30,'体内驱虫，每月一次',1,0,'2026-10-12 09:07:51','2026-10-12 09:07:51',0,NULL,NULL),
(5,'驱虫滴剂(猫)','1ml×3支',NULL,NULL,NULL,NULL,NULL,'盒','驱虫',68.00,24,'体外驱虫滴剂',1,0,'2026-10-12 09:07:51','2026-10-15 21:50:11',0,NULL,NULL),
(6,'益生菌粉','5g×10袋',NULL,NULL,NULL,NULL,NULL,'盒','保健',39.00,40,'调理肠胃',1,0,'2026-10-12 09:07:51','2026-10-12 09:07:51',0,NULL,NULL),
(7,'皮肤喷剂','30ml',NULL,NULL,NULL,NULL,NULL,'瓶','外用',52.00,20,'皮肤病外用喷剂',1,0,'2026-10-12 09:07:51','2026-10-12 09:07:51',0,NULL,NULL),
(8,'眼药水','8ml',NULL,NULL,NULL,NULL,NULL,'瓶','外用',26.00,31,'眼部消炎',1,0,'2026-10-12 09:07:51','2026-10-16 19:19:04',0,NULL,NULL),
(9,'犬四联疫苗','1头份',NULL,NULL,NULL,NULL,NULL,'支','疫苗',80.00,19,'需冷链保存',1,0,'2026-10-12 09:07:51','2026-10-20 15:56:59',0,NULL,NULL),
(10,'狂犬疫苗','1头份',NULL,NULL,NULL,NULL,NULL,'支','疫苗',50.00,20,'需冷链保存',1,0,'2026-10-12 09:07:51','2026-10-12 09:07:51',0,NULL,NULL),
(11,'狂犬疫苗','','默沙东','狂犬病',365,NULL,NULL,'支','疫苗',80.00,0,'预防：狂犬病',1,1791868978736,'2026-10-13 12:44:11','2026-10-13 13:22:58',0,NULL,NULL),
(12,'猫三联疫苗','','硕腾妙三多','猫瘟/猫鼻支/猫杯状',365,NULL,NULL,'支','疫苗',120.00,0,'预防：猫瘟/猫鼻支/猫杯状',1,0,'2026-10-13 12:44:11','2026-10-13 12:44:11',0,NULL,NULL),
(13,'犬四联疫苗','','英特威','犬瘟/细小/副流感/腺病毒',365,NULL,NULL,'支','疫苗',100.00,0,'预防：犬瘟/细小/副流感/腺病毒',1,1791868968573,'2026-10-13 12:44:11','2026-10-13 13:22:48',0,NULL,NULL),
(14,'犬八联疫苗','','辉瑞','多种犬传染病',365,NULL,NULL,'支','疫苗',130.00,0,'预防：多种犬传染病',1,0,'2026-10-13 12:44:11','2026-10-13 12:44:11',0,NULL,NULL),
(15,'猫鼻支疫苗','','英特威','猫鼻支',365,NULL,NULL,'支','疫苗',90.00,0,'预防：猫鼻支',1,1791868913638,'2026-10-13 12:44:11','2026-10-13 13:21:53',0,NULL,NULL),
(16,'犬窝咳疫苗','','拜耳','犬副流感',365,NULL,NULL,'支','疫苗',110.00,0,'预防：犬副流感',1,0,'2026-10-13 12:44:11','2026-10-13 12:44:11',0,NULL,NULL);
/*!40000 ALTER TABLE `drug` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `drug_record`
--

DROP TABLE IF EXISTS `drug_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `drug_record` (
  `record_id` int(11) NOT NULL AUTO_INCREMENT,
  `drug_id` int(11) NOT NULL,
  `drug_name` varchar(64) DEFAULT NULL,
  `change_type` tinyint(4) NOT NULL COMMENT '1å…¥åº“ 2å‘è¯å‡ºåº“ 3é€€æ¬¾å›žåº“ 4ç›˜äº/æŠ¥æŸ',
  `quantity` int(11) NOT NULL,
  `before_stock` int(11) DEFAULT NULL,
  `after_stock` int(11) DEFAULT NULL,
  `operator` varchar(50) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `supplier_id` int(11) DEFAULT NULL COMMENT 'ä¾›åº”å•†id(å…¥åº“)',
  `supplier_name` varchar(128) DEFAULT NULL COMMENT 'å†—ä½™ä¾›åº”å•†å',
  `batch_no` varchar(64) DEFAULT NULL COMMENT 'æ‰¹å·',
  `cost_price` decimal(10,2) DEFAULT NULL COMMENT 'æœ¬æ‰¹å…¥åº“æˆæœ¬ä»·(å…ƒ/å•ä½)',
  PRIMARY KEY (`record_id`),
  KEY `idx_drug` (`drug_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='è¯å“å‡ºå…¥åº“å°è´¦';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `drug_record`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `drug_record` WRITE;
/*!40000 ALTER TABLE `drug_record` DISABLE KEYS */;
INSERT INTO `drug_record` VALUES
(1,8,'眼药水',2,1,33,32,'管理员','收款发药','2026-10-15 21:50:11',NULL,NULL,NULL,NULL),
(2,5,'驱虫滴剂(猫)',2,1,25,24,'管理员','收款发药','2026-10-15 21:50:11',NULL,NULL,NULL,NULL),
(3,8,'眼药水',2,1,32,31,'管理员','收款发药','2026-10-16 19:19:04',NULL,NULL,NULL,NULL),
(4,9,'犬四联疫苗',2,1,20,19,'管理员','免疫接种出库','2026-10-20 15:56:59',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `drug_record` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `follow_up`
--

DROP TABLE IF EXISTS `follow_up`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `follow_up` (
  `follow_id` int(11) NOT NULL AUTO_INCREMENT,
  `record_id` int(11) NOT NULL COMMENT 'ç—…åŽ†id',
  `pet_id` int(11) DEFAULT NULL,
  `client_id` int(11) DEFAULT NULL,
  `follow_type` varchar(32) DEFAULT NULL COMMENT 'éšè®¿æ–¹å¼ ç”µè¯/å¾®ä¿¡/åˆ°åº—/ä¸Šé—¨',
  `content` varchar(500) DEFAULT NULL COMMENT 'éšè®¿å†…å®¹/è®¡åˆ’',
  `result` varchar(500) DEFAULT NULL COMMENT 'éšè®¿ç»“æžœ/å®¢æˆ·åé¦ˆ',
  `plan_time` datetime DEFAULT NULL COMMENT 'è®¡åˆ’éšè®¿æ—¶é—´',
  `follow_time` datetime DEFAULT NULL COMMENT 'å®žé™…éšè®¿æ—¶é—´',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0å¾…éšè®¿ 1å·²éšè®¿',
  `follow_by` varchar(50) DEFAULT NULL COMMENT 'éšè®¿äºº',
  `note` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `is_del` bigint(20) DEFAULT NULL COMMENT 'é€»è¾‘åˆ é™¤ï¼Œé»˜è®¤0ï¼Œå¡«å……åˆ é™¤æ—¥æœŸ',
  PRIMARY KEY (`follow_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='éšè®¿è®°å½•';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `follow_up`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `follow_up` WRITE;
/*!40000 ALTER TABLE `follow_up` DISABLE KEYS */;
/*!40000 ALTER TABLE `follow_up` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `foster`
--

DROP TABLE IF EXISTS `foster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `foster` (
  `foster_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '寄养表的id',
  `foster_code` varchar(16) NOT NULL COMMENT '订单编号',
  `pet_id` int(11) NOT NULL COMMENT '寄养宠物的id',
  `in_date` date DEFAULT NULL COMMENT '入托日期',
  `client_id` int(11) NOT NULL COMMENT '寄养人id',
  `foster_term` date NOT NULL COMMENT '寄养到期时间',
  `foster_money` int(11) NOT NULL DEFAULT 0 COMMENT '寄养押金',
  `daily_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '笼位费/天',
  `clean_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '清洁费/天',
  `meal_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '餐费/天',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0待入托 1寄养中 2已接回',
  `back_date` date DEFAULT NULL COMMENT '实际接回日期',
  `foster_info` varchar(64) DEFAULT NULL COMMENT '订单备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  PRIMARY KEY (`foster_id`),
  UNIQUE KEY `foster_code` (`foster_code`,`is_del`),
  KEY `pet_id` (`pet_id`),
  KEY `client_id` (`client_id`),
  CONSTRAINT `foster_ibfk_1` FOREIGN KEY (`pet_id`) REFERENCES `pet` (`pet_id`),
  CONSTRAINT `foster_ibfk_2` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='寄养表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `foster`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `foster` WRITE;
/*!40000 ALTER TABLE `foster` DISABLE KEYS */;
INSERT INTO `foster` VALUES
(1,'12301121',8,NULL,1,'2026-10-30',100,0.00,0.00,0.00,0,NULL,'寄养波斯','2023-01-12 16:44:32','2026-10-11 14:22:11',0),
(2,'12301122',32,NULL,3,'2026-10-31',800,0.00,0.00,0.00,0,NULL,'好好照顾','2023-01-12 20:04:22','2026-10-11 14:22:24',0),
(3,'12301123',33,NULL,2,'2027-10-30',5000,0.00,0.00,0.00,0,NULL,'调教ing','2023-01-12 20:08:46','2026-10-11 14:22:36',0),
(4,'12301124',5,NULL,4,'2027-10-09',300,0.00,0.00,0.00,0,NULL,'舟桥','2023-01-12 20:10:45','2026-10-11 14:23:02',0),
(5,'12301125',32,NULL,3,'2026-10-23',200,0.00,0.00,0.00,0,NULL,'有事外出','2023-01-12 21:52:28','2026-10-11 14:23:11',0),
(6,'12610166',35,NULL,14,'2026-10-31',100,0.00,0.00,0.00,0,NULL,'主人出门','2026-10-16 19:15:56','2026-10-16 19:15:56',0);
/*!40000 ALTER TABLE `foster` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `hospital_advice`
--

DROP TABLE IF EXISTS `hospital_advice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital_advice` (
  `advice_id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'åŒ»å˜±id',
  `hospital_id` int(11) NOT NULL COMMENT 'ä½é™¢è®°å½•id',
  `record_id` int(11) NOT NULL COMMENT 'å…³è”ç—…åŽ†idï¼ˆå‡ºé™¢ç»“ç®—/ç—…åŽ†è§†å›¾ç”¨ï¼‰',
  `doctor_id` int(11) DEFAULT NULL COMMENT 'ä¸‹è¾¾åŒ»ç”Ÿid',
  `doctor_name` varchar(64) DEFAULT NULL COMMENT 'ä¸‹è¾¾åŒ»ç”Ÿå§“åï¼ˆå¿«ç…§ï¼‰',
  `item_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'ç±»åž‹ï¼š1è¯å“ 2æ²»ç–— 3æŠ¤ç†',
  `drug_id` int(11) DEFAULT NULL COMMENT 'è¯å“idï¼ˆè¯å“ç±»åŒ»å˜±ï¼Œè”åŠ¨è¯å“è¡¨ï¼‰',
  `item_name` varchar(128) NOT NULL COMMENT 'é¡¹ç›®åï¼ˆè¯å“å/æ²»ç–—/æŠ¤ç†é¡¹ï¼‰',
  `dose` varchar(32) DEFAULT NULL COMMENT 'å•æ¬¡å‰‚é‡',
  `dose_unit` varchar(16) DEFAULT NULL COMMENT 'å‰‚é‡å•ä½',
  `frequency` varchar(16) DEFAULT NULL COMMENT 'é¢‘æ¬¡ï¼šQD/BID/TID/QID/æŒç»­',
  `route` varchar(32) DEFAULT NULL COMMENT 'é€”å¾„ï¼šå£æœ/æ³¨å°„/å¤–ç”¨/é™è„‰',
  `days` int(11) DEFAULT NULL COMMENT 'è®¡åˆ’å¤©æ•°',
  `fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '临时费用(一次性，出院结算计入)',
  `start_date` varchar(10) DEFAULT NULL COMMENT 'å¼€å§‹æ—¥æœŸ YYYY-MM-DD',
  `remark` varchar(255) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1å¾…æ‰§è¡Œ 2æ‰§è¡Œä¸­ 3å·²åœå˜± 4å·²å®Œæˆ',
  `stop_reason` varchar(255) DEFAULT NULL COMMENT 'åœå˜±åŽŸå› ï¼ˆåœå˜±æ—¶å¿…å¡«ï¼‰',
  `stop_time` datetime DEFAULT NULL COMMENT 'åœå˜±æ—¶é—´',
  `stop_by` varchar(50) DEFAULT NULL COMMENT 'åœå˜±æ“ä½œäºº',
  `create_time` datetime DEFAULT NULL COMMENT 'ä¸‹è¾¾æ—¶é—´',
  `update_time` datetime DEFAULT NULL COMMENT 'æ›´æ–°æ—¶é—´',
  `is_del` bigint(20) DEFAULT 0 COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`advice_id`),
  KEY `idx_hospital` (`hospital_id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='ä½é™¢åŒ»å˜±å•';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hospital_advice`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `hospital_advice` WRITE;
/*!40000 ALTER TABLE `hospital_advice` DISABLE KEYS */;
/*!40000 ALTER TABLE `hospital_advice` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `hospital_monitor`
--

DROP TABLE IF EXISTS `hospital_monitor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital_monitor` (
  `monitor_id` int(11) NOT NULL AUTO_INCREMENT,
  `hospital_id` int(11) NOT NULL COMMENT 'ä½é™¢å•id',
  `pet_id` int(11) DEFAULT NULL COMMENT 'å® ç‰©id(å†—ä½™)',
  `monitor_time` datetime DEFAULT NULL COMMENT 'è®°å½•æ—¶é—´',
  `temperature` decimal(4,1) DEFAULT NULL COMMENT 'ä½“æ¸©â„ƒ',
  `heart_rate` int(11) DEFAULT NULL COMMENT 'å¿ƒçŽ‡ æ¬¡/åˆ†',
  `respiratory_rate` int(11) DEFAULT NULL COMMENT 'å‘¼å¸ æ¬¡/åˆ†',
  `weight` decimal(6,2) DEFAULT NULL COMMENT 'ä½“é‡kg',
  `mental_state` varchar(64) DEFAULT NULL COMMENT 'ç²¾ç¥ž/çŠ¶æ€',
  `appetite` varchar(128) DEFAULT NULL COMMENT 'é£Ÿæ¬²/é¥®é£Ÿ',
  `excretion` varchar(128) DEFAULT NULL COMMENT 'æŽ’ä¾¿æŽ’å°¿',
  `medication` varchar(255) DEFAULT NULL COMMENT 'å½“æ—¥ç”¨è¯',
  `treatment` varchar(255) DEFAULT NULL COMMENT 'å¤„ç½®/æ²»ç–—',
  `note` varchar(255) DEFAULT NULL COMMENT 'å¤‡æ³¨',
  `recorder` varchar(50) DEFAULT NULL COMMENT 'è®°å½•äºº',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`monitor_id`),
  KEY `idx_hospital` (`hospital_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='ä½é™¢æ¯æ—¥ç›‘æŠ¤è®°å½•';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hospital_monitor`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `hospital_monitor` WRITE;
/*!40000 ALTER TABLE `hospital_monitor` DISABLE KEYS */;
INSERT INTO `hospital_monitor` VALUES
(1,1,35,'2026-10-19 00:00:00',37.0,120,60,6.00,'活泼','主动进食','正常','输了一天液','剪了指甲','继续观察','管理员','2026-10-19 19:09:47');
/*!40000 ALTER TABLE `hospital_monitor` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `hospital_record`
--

DROP TABLE IF EXISTS `hospital_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital_record` (
  `hospital_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '住院记录id',
  `record_id` int(11) NOT NULL COMMENT '病历id',
  `pet_id` int(11) NOT NULL COMMENT '宠物id',
  `client_id` int(11) NOT NULL COMMENT '客户id',
  `doctor_id` int(11) NOT NULL COMMENT '主治医生id',
  `room_no` varchar(32) DEFAULT NULL COMMENT '病房/床位号',
  `in_time` datetime DEFAULT NULL COMMENT '入院时间',
  `out_time` datetime DEFAULT NULL COMMENT '出院时间',
  `daily_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '日均住院费(元)',
  `bed_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '床位费/天',
  `nursing_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '护理费/天',
  `infusion_fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '输液费/天',
  `deposit` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '已收押金(累加)',
  `deposit_by` varchar(32) DEFAULT NULL COMMENT '最后押金收取人',
  `deposit_time` datetime DEFAULT NULL COMMENT '最后押金收取时间',
  `days` int(11) NOT NULL DEFAULT 0 COMMENT '住院天数',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0住院中 1已出院',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`hospital_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='住院记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hospital_record`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `hospital_record` WRITE;
/*!40000 ALTER TABLE `hospital_record` DISABLE KEYS */;
INSERT INTO `hospital_record` VALUES
(1,16,35,14,10,'一楼大笼子','2026-10-19 19:08:03',NULL,50.00,50.00,0.00,0.00,0.00,NULL,NULL,0,0,'','2026-10-19 19:08:03','2026-10-19 19:08:03',0),
(2,12,37,1,10,'A01','2026-10-23 18:08:00',NULL,70.00,50.00,10.00,10.00,0.00,NULL,NULL,0,0,'','2026-10-23 18:08:00','2026-10-23 18:08:00',0);
/*!40000 ALTER TABLE `hospital_record` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `hospital_setting`
--

DROP TABLE IF EXISTS `hospital_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `hospital_setting` (
  `id` tinyint(4) NOT NULL DEFAULT 1,
  `name` varchar(64) NOT NULL DEFAULT '' COMMENT 'åŒ»é™¢åç§°',
  `address` varchar(160) NOT NULL DEFAULT '' COMMENT 'åœ°å€',
  `phone` varchar(32) NOT NULL DEFAULT '' COMMENT 'è”ç³»ç”µè¯',
  `logo` varchar(255) DEFAULT NULL COMMENT 'logoæ–‡ä»¶å(å¤ç”¨common/download)',
  `brand_color` varchar(16) NOT NULL DEFAULT '#1E6FA8' COMMENT 'å“ç‰Œä¸»é¢˜è‰²(HEXï¼Œé»˜è®¤è“)',
  `biz_hours` varchar(64) DEFAULT '' COMMENT 'è¥ä¸šæ—¶é—´',
  `intro` varchar(255) DEFAULT '' COMMENT 'ç®€ä»‹/å®£ä¼ æ–‡æ¡ˆ',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='åŒ»é™¢åŸºç¡€ä¿¡æ¯(å•è¡Œé…ç½®)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `hospital_setting`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `hospital_setting` WRITE;
/*!40000 ALTER TABLE `hospital_setting` DISABLE KEYS */;
INSERT INTO `hospital_setting` VALUES
(1,'爱心宠物医院','北京市东港路520号','0371-123456','48c4da90-5c8e-4a4d-a82c-e5eb14ba0daa.webp','#7C5CBF','8:00-18:00','专业宠物医疗 · 用心呵护每一只毛孩子！');
/*!40000 ALTER TABLE `hospital_setting` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `image_order`
--

DROP TABLE IF EXISTS `image_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `image_order` (
  `order_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '申请单id',
  `record_id` int(11) NOT NULL COMMENT '病历id',
  `pet_id` int(11) NOT NULL COMMENT '宠物id',
  `client_id` int(11) NOT NULL COMMENT '客户id',
  `doctor_id` int(11) NOT NULL COMMENT '开单医生id',
  `item_id` int(11) NOT NULL COMMENT '项目id',
  `item_name` varchar(64) DEFAULT NULL COMMENT '冗余项目名（不受字典修改影响）',
  `group_name` varchar(32) DEFAULT NULL COMMENT '冗余分组',
  `fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '检查费用(元)',
  `exam_part` varchar(64) DEFAULT NULL COMMENT '检查部位/检查目的',
  `result` text DEFAULT NULL COMMENT '影像报告内容',
  `report_time` datetime DEFAULT NULL COMMENT '出报告时间',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0待检查 1已出报告',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `report_file` varchar(255) DEFAULT NULL COMMENT 'æŠ¥å‘Šæ–‡ä»¶(å½±åƒå›¾/PDF)è·¯å¾„',
  `is_cancel` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'æ˜¯å¦ä½œåºŸ',
  PRIMARY KEY (`order_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='影像检查申请单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image_order`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `image_order` WRITE;
/*!40000 ALTER TABLE `image_order` DISABLE KEYS */;
INSERT INTO `image_order` VALUES
(1,15,5,4,10,4,'腹部B超','B超',100.00,'','小肠强回声，疑似异物','2026-10-23 18:05:42',1,NULL,'2026-10-13 20:04:27','2026-10-13 20:04:27',0,'072b8f80-7acd-423b-a07e-8c25854b6e5c.webp',0),
(2,16,35,14,10,4,'腹部B超','B超',100.00,'',NULL,NULL,0,NULL,'2026-10-14 11:23:11','2026-10-14 11:23:11',0,NULL,0),
(3,16,35,14,10,4,'腹部B超','B超',100.00,'',NULL,NULL,0,'','2026-10-14 11:24:23','2026-10-14 11:24:23',0,NULL,0),
(4,17,35,14,10,4,'腹部B超','B超',100.00,'','太low了','2026-10-16 19:20:05',1,'','2026-10-14 16:57:39','2026-10-14 16:57:39',0,'33894ccd-baf3-4dd1-89e0-da9b9178f564.png',0),
(5,15,5,4,10,4,'腹部B超','B超',100.00,'suibian','未见异常','2026-10-16 15:42:26',1,'喘的厉害','2026-10-14 18:53:11','2026-10-14 18:53:11',0,'55b115d8-5517-4d51-9cfd-b25e0980044c.png',0),
(6,18,8,6,10,4,'腹部B超','B超',100.00,'',NULL,NULL,0,'','2026-10-17 19:49:20','2026-10-17 19:49:20',0,NULL,0),
(7,18,8,6,10,5,'心脏彩超','B超',150.00,'','好像没见问题呢','2026-10-18 19:07:16',1,'','2026-10-18 19:06:10','2026-10-18 19:06:10',0,'6a5d80e2-a6db-4371-b14b-4f0a305a45ee.png',0),
(8,19,36,1,10,7,'胸腹部CT','CT',400.00,'',NULL,NULL,0,'','2026-10-20 15:51:29','2026-10-20 15:51:29',0,NULL,0);
/*!40000 ALTER TABLE `image_order` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `image_test`
--

DROP TABLE IF EXISTS `image_test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `image_test` (
  `item_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '项目id',
  `item_name` varchar(64) NOT NULL COMMENT '项目名称',
  `group_name` varchar(32) DEFAULT NULL COMMENT '分组（X光/B超/CT等）',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价(元)',
  `detail` varchar(255) DEFAULT NULL COMMENT '说明',
  `report_tpl` text DEFAULT NULL COMMENT 'æŠ¥å‘Šæ¨¡æ¿é¡¹(JSONæ•°ç»„å­—ç¬¦ä¸²ï¼Œå¦‚ ["æ£€æŸ¥ä½“ä½","å½±åƒæ‰€è§","å½±åƒè¯Šæ–­"])',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`item_id`),
  KEY `idx_group` (`group_name`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='影像检查项目字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image_test`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `image_test` WRITE;
/*!40000 ALTER TABLE `image_test` DISABLE KEYS */;
INSERT INTO `image_test` VALUES
(1,'胸部X光片','X光',60.00,'检查心肺胸腔',NULL,NULL,NULL,0),
(2,'腹部X光片','X光',60.00,'检查腹腔脏器/异物',NULL,NULL,NULL,0),
(3,'四肢X光片','X光',80.00,'骨折/关节检查',NULL,NULL,NULL,0),
(4,'腹部B超','B超',100.00,'腹腔内脏器超声检查',NULL,NULL,NULL,0),
(5,'心脏彩超','B超',150.00,'心脏结构与血流评估',NULL,NULL,NULL,0),
(6,'头部CT','CT',300.00,'颅脑断层扫描',NULL,NULL,NULL,0),
(7,'胸腹部CT','CT',400.00,'胸腹联合断层扫描',NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `image_test` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `immune_record`
--

DROP TABLE IF EXISTS `immune_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `immune_record` (
  `immune_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '接种记录id',
  `record_id` int(11) DEFAULT NULL COMMENT '病历id(可空)',
  `pet_id` int(11) NOT NULL COMMENT '宠物id',
  `client_id` int(11) NOT NULL COMMENT '客户id',
  `doctor_id` int(11) DEFAULT NULL COMMENT '接种医生id(可空)',
  `drug_id` int(11) NOT NULL COMMENT '疫苗药品id(关联drug, drug_type=疫苗)',
  `drug_name` varchar(64) DEFAULT NULL COMMENT '冗余疫苗药品名',
  `dose_cnt` int(11) NOT NULL DEFAULT 1 COMMENT '此针为第几针',
  `vaccinate_time` datetime NOT NULL COMMENT '本次接种时间',
  `next_time` datetime DEFAULT NULL COMMENT '下一针建议时间',
  `note` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`immune_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_vaccine` (`drug_id`),
  KEY `idx_next` (`next_time`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='宠物接种档案';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `immune_record`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `immune_record` WRITE;
/*!40000 ALTER TABLE `immune_record` DISABLE KEYS */;
INSERT INTO `immune_record` VALUES
(1,12,37,1,10,14,'犬八联疫苗',1,'2026-10-13 12:06:18','2027-10-13 12:06:18','','2026-10-13 20:06:31','2026-10-13 20:06:31',0),
(2,12,37,1,10,14,'犬八联疫苗',2,'2026-10-14 05:12:57','2027-10-14 05:12:57','','2026-10-14 13:13:12','2026-10-14 13:13:12',0),
(3,16,35,14,10,10,'狂犬疫苗',1,'2026-11-04 05:13:27',NULL,'','2026-10-14 13:13:51','2026-10-14 13:13:51',0),
(4,17,35,14,10,9,'犬四联疫苗',1,'2026-10-19 12:58:56',NULL,'','2026-10-19 20:59:04','2026-10-19 20:59:04',0),
(5,19,36,1,10,9,'犬四联疫苗',1,'2026-10-20 07:56:47',NULL,'','2026-10-20 15:56:59','2026-10-20 15:56:59',0);
/*!40000 ALTER TABLE `immune_record` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `job`
--

DROP TABLE IF EXISTS `job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `job` (
  `job_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '职称id',
  `job_name` varchar(32) NOT NULL COMMENT '职称名称',
  `sort` int(11) NOT NULL DEFAULT 99 COMMENT '排序，越小越靠前',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='医院职务/职称字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `job` WRITE;
/*!40000 ALTER TABLE `job` DISABLE KEYS */;
INSERT INTO `job` VALUES
(1,'院长',1,NULL,NULL,0),
(2,'副院长',2,NULL,NULL,0),
(3,'科室主任',3,NULL,NULL,0),
(4,'副主任医师',4,NULL,NULL,0),
(5,'主治医师',5,NULL,NULL,0),
(6,'医师',6,NULL,NULL,0),
(7,'实习医师',7,NULL,NULL,0),
(8,'护士长',8,NULL,NULL,0),
(9,'护理技师',9,NULL,NULL,0),
(10,'营业员',10,NULL,NULL,0);
/*!40000 ALTER TABLE `job` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `lab_order`
--

DROP TABLE IF EXISTS `lab_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab_order` (
  `order_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '化验单id',
  `record_id` int(11) NOT NULL COMMENT '病历id',
  `pet_id` int(11) NOT NULL COMMENT '宠物id',
  `client_id` int(11) NOT NULL COMMENT '客户id',
  `doctor_id` int(11) NOT NULL COMMENT '开单医生id',
  `order_note` varchar(255) DEFAULT NULL COMMENT '检验目的/备注',
  `fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '化验费合计(元)',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0待检验 1已出结果',
  `result_note` varchar(255) DEFAULT NULL COMMENT '化验小结/结论',
  `result_by` varchar(64) DEFAULT NULL COMMENT '结果录入人',
  `create_time` datetime DEFAULT NULL COMMENT '开单时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `report_file` varchar(255) DEFAULT NULL COMMENT 'æŠ¥å‘Šæ–‡ä»¶(å›¾ç‰‡/PDF)è·¯å¾„',
  `is_cancel` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'æ˜¯å¦ä½œåºŸ',
  PRIMARY KEY (`order_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='化验单主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab_order`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `lab_order` WRITE;
/*!40000 ALTER TABLE `lab_order` DISABLE KEYS */;
INSERT INTO `lab_order` VALUES
(1,19,36,1,10,'',55.00,1,'','管理员','2026-10-22 15:51:27','2026-10-22 15:51:27',0,'2ca498e5-98ba-41c6-b367-9741dd172478.jpeg',0);
/*!40000 ALTER TABLE `lab_order` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `lab_order_item`
--

DROP TABLE IF EXISTS `lab_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab_order_item` (
  `item_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '明细id',
  `order_id` int(11) NOT NULL COMMENT '化验单id',
  `test_id` int(11) DEFAULT NULL COMMENT '字典检验项id',
  `item_name` varchar(64) NOT NULL COMMENT '冗余：项目名称',
  `item_group` varchar(32) DEFAULT NULL COMMENT '冗余：分组',
  `item_unit` varchar(32) DEFAULT NULL COMMENT '冗余：单位',
  `ref_range` varchar(64) DEFAULT NULL COMMENT '冗余：参考范围',
  `result_value` varchar(64) DEFAULT NULL COMMENT '检验结果值',
  `is_abnormal` tinyint(1) DEFAULT NULL COMMENT '是否异常',
  `result_time` datetime DEFAULT NULL COMMENT '出结果时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`item_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='化验单明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab_order_item`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `lab_order_item` WRITE;
/*!40000 ALTER TABLE `lab_order_item` DISABLE KEYS */;
INSERT INTO `lab_order_item` VALUES
(1,1,1,'白细胞计数(WBC)','血常规','10^9/L','6.0-17.0','21',1,'2026-10-23 18:03:06','2026-10-22 15:51:27','2026-10-22 15:51:27',0),
(2,1,5,'谷丙转氨酶(ALT)','生化','U/L','10-100','50',0,'2026-10-23 18:03:06','2026-10-22 15:51:27','2026-10-22 15:51:27',0);
/*!40000 ALTER TABLE `lab_order_item` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `lab_test`
--

DROP TABLE IF EXISTS `lab_test`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `lab_test` (
  `test_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '检验项id',
  `test_name` varchar(64) NOT NULL COMMENT '项目名称',
  `test_group` varchar(32) DEFAULT NULL COMMENT '分组：血常规/生化/尿检/镜检',
  `test_unit` varchar(32) DEFAULT NULL COMMENT '单位',
  `ref_range` varchar(64) DEFAULT NULL COMMENT '参考范围',
  `test_price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价(元)',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启停，1启用0停用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`test_id`),
  KEY `idx_group` (`test_group`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='检验项目字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lab_test`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `lab_test` WRITE;
/*!40000 ALTER TABLE `lab_test` DISABLE KEYS */;
INSERT INTO `lab_test` VALUES
(1,'白细胞计数(WBC)','血常规','10^9/L','6.0-17.0',15.00,1,NULL,NULL,0),
(2,'红细胞计数(RBC)','血常规','10^12/L','5.5-8.5',15.00,1,NULL,NULL,0),
(3,'血红蛋白(HGB)','血常规','g/L','120-180',15.00,1,NULL,NULL,0),
(4,'血小板计数(PLT)','血常规','10^9/L','200-500',15.00,1,NULL,NULL,0),
(5,'谷丙转氨酶(ALT)','生化','U/L','10-100',40.00,1,NULL,NULL,0),
(6,'肌酐(CREA)','生化','µmol/L','44-159',40.00,1,NULL,NULL,0),
(7,'尿素氮(BUN)','生化','mmol/L','2.5-9.6',40.00,1,NULL,NULL,0),
(8,'尿比重(SG)','尿检','','1.015-1.050',10.00,1,NULL,NULL,0),
(9,'尿蛋白(Pro)','尿检','','阴性',10.00,1,NULL,NULL,0),
(10,'粪便镜检(虫卵)','镜检','','未见',8.00,1,NULL,NULL,0);
/*!40000 ALTER TABLE `lab_test` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `medical_record`
--

DROP TABLE IF EXISTS `medical_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `medical_record` (
  `record_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '病历id',
  `appointment_id` int(11) NOT NULL COMMENT '关联挂号单',
  `pet_id` int(11) NOT NULL,
  `client_id` int(11) NOT NULL,
  `doctor_id` int(11) NOT NULL,
  `department_id` int(11) DEFAULT NULL,
  `chief_complaint` varchar(255) DEFAULT NULL COMMENT '主诉',
  `present_illness` text DEFAULT NULL COMMENT 'çŽ°ç—…å²ï¼šç—‡çŠ¶ä»Žå‡ºçŽ°åˆ°æœ¬æ¬¡å°±è¯Šçš„å‘å±•è¿‡ç¨‹',
  `past_history` text DEFAULT NULL COMMENT 'æ—¢å¾€ç—…å²ï¼šè¿‡åŽ»ä¸Žæœ¬æ¬¡åˆ¤æ–­ç›¸å…³çš„é‡è¦åŒ»ç–—ä¿¡æ¯',
  `physical_exam` varchar(255) DEFAULT NULL COMMENT '体格检查',
  `diagnosis` varchar(255) DEFAULT NULL COMMENT '诊断',
  `treatment` varchar(255) DEFAULT NULL COMMENT '治疗方案',
  `advice` varchar(255) DEFAULT NULL COMMENT '医嘱',
  `internal_note` varchar(255) DEFAULT NULL COMMENT '内部备注(顾客不可见)',
  `revisit_date` date DEFAULT NULL COMMENT '复诊日期',
  `fee_register` decimal(10,2) DEFAULT 0.00 COMMENT '挂号费',
  `fee_treat` decimal(10,2) DEFAULT 0.00 COMMENT '诊疗费',
  `is_del` bigint(20) DEFAULT 0,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `triage_level` tinyint(4) DEFAULT NULL COMMENT 'åˆ†è¯Šçº§åˆ« 1æ€¥è¯Š 2æ™®é€š 3å¤è¯Š',
  `status` tinyint(4) NOT NULL DEFAULT 0 COMMENT 'å°±è¯ŠçŠ¶æ€ï¼š0æŽ¥è¯Šä¸­ 1å·²å®Œç»“ 2å·²ä½œåºŸ',
  `finish_time` datetime DEFAULT NULL COMMENT 'å®Œç»“æ—¶é—´',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT 'ä½œåºŸåŽŸå› ',
  `weight` decimal(6,2) DEFAULT NULL COMMENT 'å°±è¯Šä½“é‡kg',
  `temperature` decimal(4,1) DEFAULT NULL COMMENT 'ä½“æ¸©â„ƒ',
  `heart_rate` int(11) DEFAULT NULL COMMENT 'å¿ƒçŽ‡ æ¬¡/åˆ†',
  `respiratory_rate` int(11) DEFAULT NULL COMMENT 'å‘¼å¸ æ¬¡/åˆ†',
  `blood_pressure` varchar(16) DEFAULT NULL COMMENT 'è¡€åŽ‹(æ”¶ç¼©/èˆ’å¼ ,å¦‚120/80)',
  `origin_record_id` int(11) DEFAULT NULL COMMENT 'å…³è”çš„åˆè¯Š/ä¸Šæ¬¡æ•°ç—…åŽ†id(å¤è¯Šé“¾)',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_appointment` (`appointment_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='电子病历';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `medical_record`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `medical_record` WRITE;
/*!40000 ALTER TABLE `medical_record` DISABLE KEYS */;
INSERT INTO `medical_record` VALUES
(1,10,39,1,4,8,'精神不佳',NULL,NULL,'体温40度','中暑','先吃药看看','保护开空调','',NULL,5.00,20.00,0,'2026-10-12 11:33:46','2026-10-16 16:27:07',NULL,1,'2026-10-12 11:33:46',NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(12,12,37,1,10,8,'咳嗽',NULL,NULL,'','未见异常','','','',NULL,10.00,0.00,0,'2026-10-13 19:48:00','2026-10-16 16:27:07',NULL,1,'2026-10-13 19:48:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(15,11,5,4,10,11,'只是看盾医生而已',NULL,NULL,'体温40','未见异常','多喝水','多喝水','主人友好','2026-10-20',10.00,20.00,0,'2026-10-13 20:04:27','2026-10-16 16:27:07',NULL,1,'2026-10-13 20:04:27',NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(16,14,35,14,10,8,'拉稀',NULL,NULL,'','快不行了','','','',NULL,10.00,0.00,0,'2026-10-14 11:23:11','2026-10-16 16:27:07',NULL,1,'2026-10-14 11:23:11',NULL,0.20,30.2,10,4,NULL,NULL),
(17,13,35,14,10,2,'左前肢无力',NULL,NULL,'','肌肉拉伤','','','',NULL,10.00,0.00,0,'2026-10-14 16:56:48','2026-10-16 16:27:07',NULL,1,'2026-10-14 16:56:48',NULL,NULL,NULL,NULL,NULL,NULL,NULL),
(18,16,8,6,10,11,'皮肤病',NULL,NULL,'','真菌感染','','','',NULL,10.00,0.00,0,'2026-10-17 11:16:56','2026-10-17 11:17:53',2,1,'2026-10-17 11:17:53',NULL,0.20,30.0,5,2,NULL,NULL),
(19,17,36,1,10,8,'打喷嚏',NULL,NULL,'精神沉郁，黏膜苍白，淋巴结肿大，跛行','肺炎','','您好，小三儿目前主要表现为打喷嚏。我们已为它安排相应检查，初步方向已记录在病历医嘱中，接下来会按方案治疗并观察反应。治疗期间如出现精神明显变差、症状加重或拒食等情况，请随时联系我们。\n对症支持：按脱水程度补液，消化道病例短期禁食后少量多餐过渡\n按检验/影像结果选择用药，避免无指征使用抗生素\n记录 24 小时进食、排便、精神与体温变化，复诊时对比评估疗效','',NULL,10.00,0.00,0,'2026-10-20 15:51:08','2026-10-22 15:51:40',NULL,1,'2026-10-22 15:51:40',NULL,0.70,30.5,10,8,'120-80',NULL);
/*!40000 ALTER TABLE `medical_record` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `member_level`
--

DROP TABLE IF EXISTS `member_level`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `member_level` (
  `level_id` int(11) NOT NULL AUTO_INCREMENT,
  `level_key` int(11) NOT NULL COMMENT 'ç­‰çº§é”® 0æ™®é€š1é“¶2é‡‘3é’»çŸ³(å¯¹åº”client.client_level)',
  `level_name` varchar(32) NOT NULL,
  `discount` decimal(3,2) NOT NULL DEFAULT 1.00 COMMENT 'æŠ˜æ‰£çŽ‡ 1=ä¸æ‰“æŠ˜',
  `recharge_bonus` decimal(3,2) NOT NULL DEFAULT 0.00 COMMENT 'å……å€¼è¿”èµ æ¯”ä¾‹',
  `points_rate` decimal(5,2) NOT NULL DEFAULT 1.00 COMMENT 'æ¶ˆè´¹ç§¯åˆ†çŽ‡ 1å…ƒ=Nåˆ†',
  `sort` int(11) NOT NULL DEFAULT 0,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_del` bigint(20) DEFAULT 0,
  PRIMARY KEY (`level_id`),
  UNIQUE KEY `uk_key` (`level_key`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='ä¼šå‘˜ç­‰çº§/ä¼šå‘˜å¡æ¨¡æ¿';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member_level`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `member_level` WRITE;
/*!40000 ALTER TABLE `member_level` DISABLE KEYS */;
INSERT INTO `member_level` VALUES
(1,0,'普通',1.00,0.00,1.00,0,1,NULL,NULL,0),
(2,1,'银卡',0.95,0.05,1.00,1,1,NULL,NULL,0),
(3,2,'金卡',0.90,0.10,2.00,2,1,NULL,NULL,0),
(4,3,'钻石',0.85,0.15,3.00,3,1,NULL,NULL,0);
/*!40000 ALTER TABLE `member_level` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `msg`
--

DROP TABLE IF EXISTS `msg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `msg` (
  `msg_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `client_id` int(11) NOT NULL COMMENT '用户ID',
  `doctor_id` int(11) NOT NULL COMMENT '医生ID',
  `is_client` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1为用户发送，0为医生',
  `msg_content` varchar(1000) DEFAULT NULL COMMENT 'æ¶ˆæ¯å†…å®¹',
  `is_img` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为图片消息',
  `msg_type` varchar(10) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/IMAGE/FILE',
  `msg_name` varchar(255) DEFAULT NULL COMMENT '文件消息的原始文件名',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'æŽ¥æ”¶æ–¹æ˜¯å¦å·²è¯»ï¼š0æœªè¯» 1å·²è¯»',
  `read_time` datetime DEFAULT NULL COMMENT 'å·²è¯»æ—¶é—´',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  PRIMARY KEY (`msg_id`),
  KEY `client_id` (`client_id`),
  KEY `doctor_id` (`doctor_id`),
  CONSTRAINT `msg_ibfk_1` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`),
  CONSTRAINT `msg_ibfk_2` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `msg`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `msg` WRITE;
/*!40000 ALTER TABLE `msg` DISABLE KEYS */;
INSERT INTO `msg` VALUES
(1,1,4,1,'在吗？',0,'TEXT',NULL,1,NULL,'2023-02-26 12:46:22','2023-02-25 12:19:46',1792116654096),
(2,1,4,0,'你好，在的',0,'TEXT',NULL,1,NULL,'2023-02-26 12:46:24','2023-02-25 12:20:02',1792116654096),
(3,1,2,0,'你好，有什么需要帮助的吗？',0,'TEXT',NULL,1,NULL,'2023-02-26 12:46:26','2023-02-26 10:35:20',1792116654096),
(4,1,4,0,'你有什么想问的',0,'TEXT',NULL,1,NULL,'2023-02-26 12:54:24','2023-02-26 12:54:24',1792116654096),
(5,1,4,1,'就是那个',0,'TEXT',NULL,1,NULL,'2023-02-26 12:57:06','2023-02-26 12:57:06',1792116654096),
(6,1,4,1,'我家的鱼的鳞片都立起来了',0,'TEXT',NULL,1,NULL,'2023-02-26 12:57:18','2023-02-26 12:57:18',1792116654096),
(7,1,4,1,'昨天还好好的',0,'TEXT',NULL,1,NULL,'2023-02-26 12:58:32','2023-02-26 12:58:32',1792116654096),
(8,2,4,1,'你好啊',0,'TEXT',NULL,1,NULL,'2023-03-03 21:45:15','2023-03-03 21:45:15',1792116654096),
(9,1,1,1,'童医生，你好',0,'TEXT',NULL,1,NULL,'2023-03-03 22:55:24','2023-03-03 22:55:24',1792116654096),
(10,1,2,1,'暂时没有',0,'TEXT',NULL,1,NULL,'2023-03-03 23:20:04','2023-03-03 23:20:04',1792116654096),
(11,1,4,0,'看你的描述，可能是立鳞病',0,'TEXT',NULL,1,NULL,'2023-03-06 13:14:24','2023-03-06 13:14:24',1792116654096),
(12,1,4,0,'水质太差了就会这样',0,'TEXT',NULL,1,NULL,'2023-03-06 13:15:12','2023-03-06 13:15:12',1792116654096),
(13,1,1,0,'你好',0,'TEXT',NULL,1,NULL,'2023-03-06 13:15:36','2023-03-06 13:15:36',1792116654096),
(14,1,1,1,'我的鱼的鳞片为什么都竖起来了',0,'TEXT',NULL,1,NULL,'2023-03-06 21:54:27','2023-03-06 21:54:27',1792116654096),
(15,1,1,1,'前两天还好好的',0,'TEXT',NULL,1,NULL,'2023-03-06 21:54:57','2023-03-06 21:54:57',1792116654096),
(16,1,1,0,'可能染上了立鳞病',0,'TEXT',NULL,1,NULL,'2023-03-06 21:56:26','2023-03-06 21:56:26',1792116654096),
(17,1,1,1,'立鳞病？那是什么',0,'TEXT',NULL,1,NULL,'2023-03-07 16:09:39','2023-03-07 16:09:39',1792116654096),
(18,1,1,0,'其会使病鱼体表肿胀，部分鳞片向外张开使鳞片竖起。',0,'TEXT',NULL,1,NULL,'2023-03-07 16:11:48','2023-03-07 16:11:48',1792116654096),
(19,1,1,1,'真的吗',0,'TEXT',NULL,1,NULL,'2023-03-07 16:12:06','2023-03-07 16:12:06',1792116654096),
(20,1,1,1,'那怎么办啊',0,'TEXT',NULL,1,NULL,'2023-03-07 16:14:37','2023-03-07 16:14:37',1792116654096),
(21,1,4,1,'啊这。。。。。',0,'TEXT',NULL,1,NULL,'2023-03-07 16:20:52','2023-03-07 16:20:52',1792116654096),
(22,1,2,0,'好的，有需要随时叫我',0,'TEXT',NULL,1,NULL,'2023-03-07 16:30:48','2023-03-07 16:30:48',1792116654096),
(23,1,2,1,'❥(^_-)',0,'TEXT',NULL,1,NULL,'2023-03-07 16:31:05','2023-03-07 16:31:05',1792116654096),
(24,1,1,0,'你发张照片我看看',0,'TEXT',NULL,1,NULL,'2023-03-07 17:16:24','2023-03-07 17:16:24',1792116654096),
(25,1,1,1,'这样行吗？',0,'TEXT',NULL,1,NULL,'2023-03-08 13:30:33','2023-03-08 13:30:33',1792116654096),
(26,1,1,1,'ms-img-01.png',1,'IMAGE',NULL,1,NULL,'2023-03-09 12:58:19','2023-03-09 12:58:19',1792116654096),
(27,1,1,0,'ms-img-02.png',1,'IMAGE',NULL,1,NULL,'2023-03-09 12:58:51','2023-03-09 12:58:51',1792116654096),
(28,1,1,1,'ms-img-03.png',1,'IMAGE',NULL,1,NULL,'2023-03-09 12:59:03','2023-03-09 12:59:03',1792116654096),
(29,1,1,1,'呵呵',0,'TEXT',NULL,1,NULL,'2026-10-11 12:01:04','2026-10-11 12:01:04',1792117189202),
(30,1,1,0,'有什么问题吗？',0,'TEXT',NULL,1,NULL,'2026-10-11 12:01:30','2026-10-11 12:01:30',1792117189202),
(31,1,1,1,'你好\n',0,'TEXT',NULL,1,NULL,'2026-10-11 19:59:49','2026-10-11 19:59:49',1792117189202),
(32,1,1,0,'你有什么问题吗？',0,'TEXT',NULL,1,NULL,'2026-10-11 20:00:39','2026-10-11 20:00:39',1792117189202),
(33,1,1,1,'没有\n',0,'TEXT',NULL,1,NULL,'2026-10-11 20:00:52','2026-10-11 20:00:52',1792117189202),
(34,1,4,1,'呵呵',0,'TEXT',NULL,1,NULL,'2026-10-11 20:09:19','2026-10-11 20:09:19',1792117189202),
(35,1,10,1,'你好，在吗？',0,'TEXT',NULL,1,NULL,'2026-10-11 20:10:58','2026-10-11 20:10:58',1792117189202),
(36,1,10,1,'在\n吗\n？',0,'TEXT',NULL,1,NULL,'2026-10-11 20:11:13','2026-10-11 20:11:13',1792117189202),
(37,1,10,0,'不在',0,'TEXT',NULL,1,NULL,'2026-10-11 20:11:58','2026-10-11 20:11:58',1792117189202),
(38,1,10,1,'1',0,'TEXT',NULL,1,NULL,'2026-10-11 20:13:26','2026-10-11 20:13:26',1792117189202),
(39,1,10,1,'哥哥',0,'TEXT',NULL,1,NULL,'2026-10-11 20:18:48','2026-10-11 20:18:48',1792117189202),
(40,1,10,1,'么四？',0,'TEXT',NULL,1,NULL,'2026-10-11 20:20:14','2026-10-11 20:20:14',1792117189202),
(41,1,10,0,'嗯？',0,'TEXT',NULL,1,NULL,'2026-10-11 20:20:42','2026-10-11 20:20:42',1792117189202),
(42,14,10,1,'呜咽',0,'TEXT',NULL,1,NULL,'2026-10-13 20:56:19','2026-10-13 20:56:19',1792117189202),
(43,14,10,1,'哈啰，在吗？',0,'TEXT',NULL,1,NULL,'2026-10-13 20:56:26','2026-10-13 20:56:26',1792117189202),
(44,14,10,0,'dc94b4d5-5607-4956-bba0-38940eed0d9a.jpg',1,'IMAGE',NULL,1,NULL,'2026-10-14 15:00:20','2026-10-14 15:00:20',1792117189202),
(45,14,10,0,'能看清图片吗？',0,'TEXT',NULL,1,NULL,'2026-10-14 15:00:50','2026-10-14 15:00:50',1792117189202),
(46,14,10,1,'图片不能放大，所以看不清的',0,'TEXT',NULL,1,NULL,'2026-10-14 15:02:22','2026-10-14 15:02:22',1792117189202),
(47,14,10,1,'fccdccd7-1ef9-4a72-bb96-c087fd94a48c.webp',1,'IMAGE',NULL,1,NULL,'2026-10-16 09:45:40','2026-10-16 09:45:40',1792118050106),
(48,14,10,1,'bc5c9eed-47b8-4c2d-89e4-bc0d32c72c70.webp',1,'IMAGE',NULL,1,NULL,'2026-10-16 09:46:47','2026-10-16 09:46:47',1792118050106),
(49,14,10,1,'665d2ba4-d6ca-47ab-9aca-67e12bbb3432.png',0,'FILE','截图+2026-08-03+17-13-32.png',1,NULL,'2026-10-16 10:05:57','2026-10-16 10:05:57',1792118050106),
(50,14,10,0,'8e9c13e6-627f-4039-a2a0-cc75daa567bc.doc',0,'FILE','易宠大型版系统安装及设置说明2.doc',1,NULL,'2026-10-16 10:06:40','2026-10-16 10:06:40',1792118050106),
(51,14,10,1,'5c3853d4-f1b2-4865-91cb-d4b345accfe4.webp',1,'IMAGE',NULL,1,NULL,'2026-10-16 10:17:58','2026-10-16 10:17:58',1792118050106),
(52,14,10,1,'885cb092-b27f-4671-97f4-f5cc7f92bac6.md',0,'FILE','README.md',1,NULL,'2026-10-16 10:18:47','2026-10-16 10:18:47',1792118050106),
(53,14,10,1,'你好',0,'TEXT',NULL,1,NULL,'2026-10-16 12:12:00','2026-10-16 12:12:00',1792124993564),
(54,14,10,0,'你好，请问有什么问题？',0,'TEXT',NULL,1,NULL,'2026-10-16 12:12:35','2026-10-16 12:12:35',1792124993564),
(55,14,10,1,'9f4a078b-df69-40ca-8956-7f65f04c3456.webp',1,'IMAGE',NULL,1,NULL,'2026-10-16 12:13:09','2026-10-16 12:13:09',1792124993564),
(56,14,10,1,'帮我看看这段文字什么意思？',0,'TEXT',NULL,1,NULL,'2026-10-16 12:13:11','2026-10-16 12:13:11',1792124993564),
(57,14,10,1,'8a8b6c15-ce70-4fed-a4ae-847671d91193.md',0,'FILE','README.md',1,NULL,'2026-10-16 12:14:26','2026-10-16 12:14:26',1792124993564),
(58,14,10,1,'f4f8f281-6657-4f79-a3d2-4029c1d2bc34.html',0,'FILE','index.html',1,NULL,'2026-10-16 12:14:32','2026-10-16 12:14:32',1792124993564),
(59,14,10,1,'你好',0,'TEXT',NULL,1,NULL,'2026-10-16 12:31:23','2026-10-16 12:31:23',1792150459239),
(60,14,10,1,'嘿',0,'TEXT',NULL,1,NULL,'2026-10-16 12:36:15','2026-10-16 12:36:15',1792150459239),
(61,14,1,1,'你好',0,'TEXT',NULL,1,NULL,'2026-10-16 12:37:30','2026-10-16 12:37:30',1792150459239),
(62,14,10,0,'嘿',0,'TEXT',NULL,1,NULL,'2026-10-16 13:01:27','2026-10-16 13:01:27',1792150459239),
(63,14,10,1,'我就试试好用否',0,'TEXT',NULL,1,NULL,'2026-10-16 13:02:01','2026-10-16 13:02:01',1792150459239),
(64,14,10,1,'怎么不回复 ？',0,'TEXT',NULL,1,NULL,'2026-10-16 13:02:23','2026-10-16 13:02:23',1792150459239),
(65,14,10,1,'哈哈',0,'TEXT',NULL,1,'2026-10-16 13:41:29','2026-10-16 13:20:16','2026-10-16 13:20:16',1792150459239),
(66,14,10,1,'有提醒吗？',0,'TEXT',NULL,1,'2026-10-16 13:47:12','2026-10-16 13:42:20','2026-10-16 13:42:20',1792150459239),
(67,14,10,1,'94fa72cd-eab3-4f71-b711-7465aeb59db9.webp',1,'IMAGE',NULL,1,'2026-10-16 13:47:12','2026-10-16 13:42:38','2026-10-16 13:42:38',1792150459239),
(68,14,10,1,'2caa8208-751b-4e08-b215-018857883326.txt',0,'FILE','robots.txt',1,'2026-10-16 13:47:12','2026-10-16 13:43:27','2026-10-16 13:43:27',1792150459239),
(69,1,10,1,'请问在吗？',0,'TEXT',NULL,1,'2026-10-16 13:47:34','2026-10-16 13:46:08','2026-10-16 13:46:08',1792150459239),
(70,1,10,0,'cdf92055-da05-4235-9492-7af8c1240352.webp',1,'IMAGE',NULL,1,'2026-10-16 13:55:31','2026-10-16 13:47:59','2026-10-16 13:47:59',1792150459239),
(71,1,10,0,'a0387a27-0138-4350-8cb1-54812d0b4562.webp',1,'IMAGE',NULL,1,'2026-10-16 13:59:05','2026-10-16 13:55:58','2026-10-16 13:55:58',1792150459239),
(72,1,10,0,'111',0,'TEXT',NULL,1,'2026-10-16 14:07:39','2026-10-16 14:06:39','2026-10-16 14:06:39',1792150459239),
(73,1,10,1,'图片不太清楚哦，我给个大一点的吧',0,'TEXT',NULL,1,'2026-10-16 18:55:00','2026-10-16 18:54:36','2026-10-16 18:54:36',1792150459239),
(74,1,10,1,'aadda01e-a312-4818-9cdf-35a61f87a8e3.webp',1,'IMAGE',NULL,1,'2026-10-16 18:55:00','2026-10-16 18:54:49','2026-10-16 18:54:49',1792150459239),
(75,1,10,1,'c3a60a17-7573-40cf-99a1-dd783414b957.webp',1,'IMAGE',NULL,1,'2026-10-17 20:25:53','2026-10-17 20:25:46','2026-10-17 20:25:46',0);
/*!40000 ALTER TABLE `msg` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `notice`
--

DROP TABLE IF EXISTS `notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `notice` (
  `notice_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '公告id',
  `notice_title` varchar(64) NOT NULL COMMENT '公告标题',
  `notice_file` varchar(64) NOT NULL COMMENT '文本文件名称',
  `creat_id` int(11) NOT NULL COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_id` int(11) DEFAULT NULL COMMENT '修改人id',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_disable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否禁用',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  `notice_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0é¡¾å®¢å…¬å‘Š(ä¸»é¡µå¯è§) 1ç³»ç»Ÿæé†’(ä»…å‘˜å·¥å¯è§)',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT 'æŽ’åºæƒé‡ï¼Œå·å¤§è¶Šé å‰',
  PRIMARY KEY (`notice_id`),
  KEY `creat_id` (`creat_id`),
  KEY `update_id` (`update_id`),
  CONSTRAINT `notice_ibfk_1` FOREIGN KEY (`update_id`) REFERENCES `doctor` (`doctor_id`),
  CONSTRAINT `notice_ibfk_2` FOREIGN KEY (`creat_id`) REFERENCES `doctor` (`doctor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notice`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `notice` WRITE;
/*!40000 ALTER TABLE `notice` DISABLE KEYS */;
INSERT INTO `notice` VALUES
(1,'宠物医院开业公告','notice-1.txt',1,'2023-01-02 17:22:55',1,'2026-10-12 20:39:29',0,1792135825863,0,1),
(4,'免疫提醒！','4d7dcc2e-7465-49aa-b476-c85a255162a9.txt',1,'2023-01-11 17:52:06',10,'2026-10-16 14:53:24',0,0,0,4),
(5,'做好宠物的疾病防治','f91d4670-9d59-44c8-af5d-b74ef6f4dc76.txt',1,'2023-01-11 17:53:56',1,'2026-10-12 20:39:21',0,0,0,5),
(6,'dd','fa9c33fc-1bb0-446f-8a76-5aed34228976.txt',1,'2023-01-11 17:54:22',1,NULL,0,1690468063801,0,6),
(7,'开始采用宠物医院管理系统','4b50890c-a992-4d15-b5da-1dc26306df12.txt',1,'2023-01-11 19:08:19',1,'2026-10-12 20:39:01',0,0,0,7),
(8,'加强节粮减损，杜绝粮食浪费','9a9214e5-333c-4146-9f96-4a5c00b52215.txt',1,'2023-01-11 19:09:20',1,'2026-10-12 20:39:08',0,0,0,8),
(9,'宠物日常预防与护理','af588adb-9a60-4506-b058-98012ad9ea2c.txt',1,'2023-01-15 19:48:42',10,'2026-10-16 14:54:39',0,0,0,9),
(10,'开业公告！','c4493578-aa00-4677-afd7-d1e31ec1b920.txt',10,'2026-10-16 15:31:41',10,'2026-10-16 15:31:41',0,0,0,10);
/*!40000 ALTER TABLE `notice` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `package_item`
--

DROP TABLE IF EXISTS `package_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `package_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
  `package_id` int(11) NOT NULL COMMENT 'å¥—é¤id',
  `item_type` varchar(8) NOT NULL COMMENT 'lab æ£€éªŒ / image å½±åƒ',
  `item_id` int(11) NOT NULL COMMENT 'é¡¹ç›®id(æ£€éªŒé¡¹/å½±åƒé¡¹)',
  `item_name` varchar(128) NOT NULL COMMENT 'é¡¹ç›®åå¿«ç…§',
  `item_price` decimal(10,2) DEFAULT NULL COMMENT 'é¡¹ç›®å•ä»·å¿«ç…§(ç»„åˆæ—¶æ±‡æ€»åŽŸä»·å‚è€ƒ)',
  `create_time` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
  `is_del` bigint(20) DEFAULT 0 COMMENT 'é€»è¾‘åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_package` (`package_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='å¥—é¤åŒ…å«é¡¹ç›®ï¼ˆæ£€éªŒ/å½±åƒï¼‰';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `package_item`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `package_item` WRITE;
/*!40000 ALTER TABLE `package_item` DISABLE KEYS */;
INSERT INTO `package_item` VALUES
(1,1,'lab',9,'尿蛋白(Pro)',10.00,'2026-10-20 18:23:21',0),
(2,1,'lab',1,'白细胞计数(WBC)',15.00,'2026-10-20 18:23:21',0),
(3,1,'image',7,'胸腹部CT',400.00,'2026-10-20 18:23:21',0);
/*!40000 ALTER TABLE `package_item` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `pet`
--

DROP TABLE IF EXISTS `pet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet` (
  `pet_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '宠物的id',
  `pet_name` varchar(16) NOT NULL DEFAULT '未名' COMMENT '宠物姓名',
  `pet_variety` varchar(32) NOT NULL DEFAULT '未知' COMMENT '宠物的品种',
  `pet_sex` tinyint(1) NOT NULL DEFAULT 0 COMMENT '宠物性别，0母1公',
  `pet_birthday` date DEFAULT NULL COMMENT '宠物生日',
  `pet_status` varchar(16) DEFAULT '健康' COMMENT '宠物状态',
  `pet_info` varchar(255) DEFAULT '无' COMMENT '宠物动态',
  `client_id` int(11) DEFAULT NULL COMMENT '宠物主人id',
  `pet_source` tinyint(4) NOT NULL DEFAULT 0 COMMENT '宠物来源，0后台录入 1顾客自建',
  `pet_photo` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT 'defaultPet.jpg' COMMENT '宠物照片名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除，默认0，填充删除日期',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用 0停用 1正常',
  `pet_birthday_estimated` tinyint(1) NOT NULL DEFAULT 0 COMMENT '生日是否估算 0精确 1估算',
  `pet_color` varchar(16) DEFAULT NULL COMMENT '毛色',
  `pet_weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `pet_sterilization` tinyint(1) NOT NULL DEFAULT 0 COMMENT '绝育 0否 1是',
  `pet_chip` varchar(32) DEFAULT NULL COMMENT '芯片号',
  `pet_allergy` varchar(255) DEFAULT NULL COMMENT '过敏史',
  `pet_history` varchar(255) DEFAULT NULL COMMENT '既往病史',
  `pet_alive` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在世 1是 0否',
  `pet_note` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`pet_id`),
  KEY `fk_pet_client` (`client_id`) USING BTREE,
  CONSTRAINT `fk_pet_client` FOREIGN KEY (`client_id`) REFERENCES `client` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='宠物信息表，外键用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pet`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `pet` WRITE;
/*!40000 ALTER TABLE `pet` DISABLE KEYS */;
INSERT INTO `pet` VALUES
(1,'saber','金毛犬',0,'2005-01-13','怀孕待产',NULL,NULL,0,NULL,'2022-11-30 15:52:47','2022-12-01 15:52:53',1690468063801,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(2,'甘雨','椰羊',0,'1905-01-13','频繁掉毛',NULL,NULL,0,NULL,'2022-11-30 15:53:07','2022-12-01 15:53:10',1690468063801,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(3,'心海','观赏鱼',1,'2018-10-10','非常健康','它有了温暖的家，感恩',1,0,'f66eb858-e13c-438a-b235-8889d2d416be.png','2022-11-30 15:55:04','2026-10-11 18:04:37',0,1,0,'红色',0.20,1,NULL,NULL,NULL,1,'人很好'),
(4,'神里凌华','柴犬',0,'2004-01-13','肛裂',NULL,NULL,0,NULL,'2022-11-30 15:56:41','2022-12-01 15:56:48',1690468063801,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(5,'雷电小兵','吉娃娃',1,'2024-10-10','绝育手术','这宠物十分开心',4,0,'a78d73bc-a557-41e0-8a8f-8ddc5c22e7ce.png','2022-11-30 15:58:30','2026-10-11 18:04:35',0,1,0,NULL,NULL,1,NULL,NULL,NULL,1,NULL),
(6,'笔试测试调试','但是',1,'2022-12-29','良好',NULL,NULL,0,NULL,'2022-12-23 10:55:30','2022-12-23 10:55:30',1690468063801,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(7,'波一斯','波斯猫',1,'2025-12-10','频繁掉毛','等待有缘人领养',2,0,'boss-cat.jpg','2022-12-23 11:03:06','2026-10-10 16:48:21',1791713047146,1,0,NULL,NULL,1,NULL,NULL,NULL,1,NULL),
(8,'波二斯','波斯猫',1,'2026-02-10','良好','等待有缘人领养',6,0,'fd1cdd8a-3411-4b95-a20d-5ad3993c94af.jpg','2022-12-23 11:07:29','2026-10-11 18:04:33',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(9,'波三斯','波斯猫',0,'2013-12-23','频繁掉毛','它有了温暖的家，感恩',14,0,'a943938f-7d21-49d3-a909-22f2c2dc2c9b.jpg','2022-12-23 11:23:08','2026-10-11 18:04:31',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(10,'波四斯','波斯猫',0,'2012-12-03','良好','这宠物十分开心',2,0,'92d90558-d92f-4dd8-bca3-5088825e0729.jpg','2022-12-23 11:25:13','2023-04-03 22:04:39',1791713047166,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(11,'大橘莹','橘猫',0,'2007-12-23','体重超标','等待有缘人领养',NULL,0,'ju-cat.jpg','2022-12-23 11:36:09','2023-02-18 14:28:29',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(12,'二橘','橘猫',1,'2009-12-24','体重超标','这宠物十分开心',3,0,'280906db-7875-44f7-babc-da106823b59e.jpg','2022-12-23 11:40:07','2023-04-03 22:06:22',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(13,'三橘','橘猫',0,'2009-12-23','良好','等待有缘人领养',NULL,0,'42e28f7b-c1ab-49fd-a486-4f4ce35c565b.jpg','2022-12-23 11:42:23','2023-04-03 22:06:33',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(14,'四橘','橘猫',1,'2010-12-23','需要绝育','它有了温暖的家，感恩',NULL,0,'3e56e4f8-5459-4559-85af-f7b0ecc2ff1d.jpg','2022-12-23 11:43:21','2023-04-03 22:06:42',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(15,'五橘','橘猫',0,'2012-12-23','头部肿胀',NULL,NULL,0,'','2022-12-23 11:49:51','2022-12-23 11:49:51',1690468063801,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(16,'五橘','橘猫',0,'2012-12-23','头部肿胀','等待有缘人领养',NULL,0,'cf037563-88bd-40dd-b139-908b8ac72fbd.jpg','2022-12-23 11:49:59','2023-04-03 22:06:56',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(17,'六橘','橘猫',1,'2012-12-23','发情期','等待有缘人领养',NULL,0,'a6f7dc61-0f4f-47b9-bfee-535d5c6d80f6.jpg','2022-12-23 16:34:36','2023-04-03 22:07:47',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(18,'狸大花','狸花猫',0,'2012-12-23','发情期','等待有缘人领养',NULL,0,'li-hua.jpg','2022-12-23 17:25:49','2023-02-18 14:28:51',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(19,'狸二花','狸花猫',1,'2013-12-23','需要绝育','这宠物十分开心',3,0,'f0050984-abe6-4091-bbbf-c34efef0a70c.jpg','2022-12-23 17:28:29','2023-04-03 22:09:11',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(20,'狸三花','狸花猫',0,'2012-12-23','发情期','等待有缘人领养',NULL,0,'0a7107fe-bc9b-4a6a-a55e-f32b81c0aae1.jpg','2022-12-23 18:36:17','2023-04-03 22:09:21',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(21,'狸四花','狸花猫',0,'2013-12-30','需要绝育','等待有缘人领养',NULL,0,'a4ef72a4-c8a3-4ae9-a830-adf0861763e9.jpg','2022-12-23 19:07:33','2023-04-03 22:09:27',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(22,'狸五花','狸花猫',0,'2018-12-23','体重超标','等待有缘人领养',NULL,0,'ae30b79c-3ff9-4890-83ef-7c88b842128f.jpg','2022-12-23 19:34:04','2023-04-03 22:10:32',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(23,'狸六花','狸花猫',0,'2022-12-24','频繁掉毛','它有了温暖的家，感恩',NULL,0,'bd8ca20c-6aa7-42eb-8413-d6b041e374f0.jpg','2022-12-23 19:37:43','2023-04-03 22:10:40',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(24,'金大毛','金毛犬是',0,'2012-12-24','良好','它有了温暖的家，感恩',NULL,0,'jin-mao.jpg','2022-12-24 10:51:45','2023-01-07 18:56:24',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(25,'金二毛','金毛犬',0,'2014-12-24','发情期','等待有缘人领养',NULL,0,'c0aff136-1496-4837-a90e-39438a276852.png','2022-12-24 11:13:54','2023-04-03 22:11:45',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(26,'金三毛','金毛犬',1,'2012-12-31','发情期','等待有缘人领养',NULL,0,'f72b2d65-0d23-47bc-acf0-7f60be5e91e1.png','2022-12-29 15:38:38','2023-04-03 22:11:52',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(27,'金四毛','金毛犬',0,'2012-12-29','乱叫','等待有缘人领养',NULL,0,'328d2c1b-d6e4-4554-9b80-cd15a43d7541.png','2022-12-29 15:52:09','2023-04-03 22:12:00',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(28,'金五毛','金毛犬',1,'2022-12-30','体重超标','它有了温暖的家，感恩',NULL,0,'jin-mao.jpg','2022-12-30 23:10:04','2023-05-21 14:38:21',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(29,'布大偶','布偶猫',0,'2012-12-30','脱水','等待有缘人领养',NULL,0,'bu-ou.jpg','2022-12-30 23:10:22','2023-01-09 13:01:47',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(30,'金七毛','金毛犬',0,'2022-12-30','频繁掉毛',NULL,NULL,0,'','2022-12-30 23:19:56','2022-12-30 23:19:56',1690468063801,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(31,'布小偶','布偶猫',0,'2012-12-31','频繁掉毛','等待有缘人领养',NULL,0,'3d61f5ee-cb55-4350-8c93-e3c66b14acc2.png','2022-12-30 23:22:07','2023-04-03 22:12:48',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(32,'布偶二','布偶猫',1,'2012-12-30','良好','这宠物十分开心',3,0,'3b5d2e67-8af1-4a58-9bed-91d5876a1d97.png','2022-12-30 23:24:18','2023-04-03 22:12:54',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(33,'八重神子','藏狐',0,'2013-01-01','需要绝育','这宠物十分开心',2,0,'05b8baa0-6488-433b-848b-03bb098b256d.png','2023-01-01 21:00:05','2023-04-03 22:14:40',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(34,'九重狐狸','红狐狸',0,'2013-01-10','良好','等待有缘人领养',NULL,0,'94af26d2-80ff-4cdf-85ab-330c9a436c3c.png','2023-01-10 21:10:44','2023-04-03 22:15:28',0,1,0,NULL,NULL,0,NULL,NULL,NULL,1,NULL),
(35,'三三','测试',1,'2024-10-01','健康','拉了几天稀，瘦了2斤',14,0,'defaultPet.jpg','2026-10-10 18:32:52','2026-10-11 14:47:18',0,1,0,'白色',3.50,1,NULL,NULL,NULL,1,NULL),
(36,'小三儿','田园猫',0,'2023-10-11','健康','暂无近况',1,1,'defaultPet.jpg','2026-10-11 21:01:44','2026-10-11 21:01:44',0,1,1,'三花',NULL,0,NULL,NULL,NULL,1,NULL),
(37,'小四','田园犬',1,'2025-10-01','还行','暂无近况',1,1,'defaultPet.jpg','2026-10-11 21:15:56','2026-10-11 21:15:56',0,1,0,'白色',3.00,1,NULL,NULL,NULL,1,NULL),
(38,'小五子','金毛',0,'2021-10-12','健康','暂无近况',1,1,'6fa52e86-0b20-44f2-a5ca-499804cd5650.png','2026-10-12 11:19:28','2026-10-12 11:21:09',1791775602151,1,1,'黄色',35.00,1,NULL,'对鸡肉过敏','以前得过皮肤病',1,'经常咳嗽'),
(39,'小五子','金毛',0,'2023-10-12','尚可','暂无近况',1,1,'e2268223-a046-43c6-9132-f7829699abf4.png','2026-10-12 11:28:29','2026-10-12 11:28:39',0,1,1,'黄色',36.00,0,NULL,'对鸡肉过敏','以前得过关节炎',1,'不爱动，特能吃'),
(40,'旺财','拉布拉多',1,'2023-10-23','健康','暂无近况',18,1,'0ecd6188-d5aa-4814-8f7b-cad5bb6881bb.webp','2026-10-23 18:20:26','2026-10-23 18:23:18',0,1,1,'黄',37.00,0,NULL,NULL,NULL,1,NULL);
/*!40000 ALTER TABLE `pet` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `pet_transfer`
--

DROP TABLE IF EXISTS `pet_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `pet_transfer` (
  `transfer_id` int(11) NOT NULL AUTO_INCREMENT,
  `pet_id` int(11) NOT NULL,
  `from_client_id` int(11) DEFAULT NULL COMMENT '原主人id，可能为空（无主宠物）',
  `to_client_id` int(11) DEFAULT NULL COMMENT '新主人id，撤回/入栏时为空',
  `transfer_date` datetime NOT NULL COMMENT '过户时间',
  `reason` varchar(255) DEFAULT NULL COMMENT '过户原因',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`transfer_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_to_client` (`to_client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='宠物过户历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pet_transfer`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `pet_transfer` WRITE;
/*!40000 ALTER TABLE `pet_transfer` DISABLE KEYS */;
INSERT INTO `pet_transfer` VALUES
(1,3,1,14,'2026-10-11 14:45:00','家里有人生病不让养了','2026-10-11 14:45:24','2026-10-11 14:45:24'),
(2,3,14,1,'2026-10-11 14:52:00','不听话，再送回来','2026-10-11 14:52:58','2026-10-11 14:52:58'),
(3,9,NULL,1,'2026-10-11 15:10:32','领养','2026-10-11 15:10:32','2026-10-11 15:10:32'),
(4,9,1,NULL,'2026-10-11 15:43:18','撤回领养','2026-10-11 15:43:18','2026-10-11 15:43:18'),
(5,9,NULL,1,'2026-10-11 15:49:01','领养','2026-10-11 15:49:01','2026-10-11 15:49:01'),
(6,9,1,NULL,'2026-10-11 15:49:07','撤回领养','2026-10-11 15:49:07','2026-10-11 15:49:07'),
(7,9,NULL,1,'2026-10-11 15:49:11','领养','2026-10-11 15:49:11','2026-10-11 15:49:11'),
(8,9,1,NULL,'2026-10-11 15:49:15','撤回领养','2026-10-11 15:49:15','2026-10-11 15:49:15'),
(9,9,NULL,1,'2026-10-11 16:12:32','领养','2026-10-11 16:12:32','2026-10-11 16:12:32'),
(10,9,1,NULL,'2026-10-11 16:12:40','撤回领养','2026-10-11 16:12:40','2026-10-11 16:12:40'),
(11,9,NULL,1,'2026-10-11 17:40:41','领养：还想再试试','2026-10-11 17:40:41','2026-10-11 17:40:41'),
(12,9,1,NULL,'2026-10-11 17:40:49','撤回领养','2026-10-11 17:40:49','2026-10-11 17:40:49'),
(13,14,1,NULL,'2026-10-11 17:40:53','撤回领养','2026-10-11 17:40:53','2026-10-11 17:40:53'),
(14,24,6,NULL,'2026-10-11 17:40:56','撤回领养','2026-10-11 17:40:56','2026-10-11 17:40:56'),
(15,23,3,NULL,'2026-10-11 17:55:12','撤回领养','2026-10-11 17:55:12','2026-10-11 17:55:12'),
(16,28,6,NULL,'2026-10-11 17:55:14','撤回领养','2026-10-11 17:55:14','2026-10-11 17:55:14'),
(17,9,NULL,1,'2026-10-11 17:55:22','领养：还想再试试','2026-10-11 17:55:22','2026-10-11 17:55:22'),
(18,22,3,NULL,'2026-10-11 17:56:43','撤回领养','2026-10-11 17:56:43','2026-10-11 17:56:43'),
(19,34,3,NULL,'2026-10-11 17:56:45','撤回领养','2026-10-11 17:56:45','2026-10-11 17:56:45'),
(20,26,1,NULL,'2026-10-11 17:56:48','撤回领养','2026-10-11 17:56:48','2026-10-11 17:56:48'),
(21,9,1,NULL,'2026-10-11 17:59:13','撤回领养','2026-10-11 17:59:13','2026-10-11 17:59:13'),
(22,3,1,NULL,'2026-10-11 19:43:45','入栏：主人托管','2026-10-11 19:43:45','2026-10-11 19:43:45'),
(23,3,NULL,1,'2026-10-11 20:49:59','领养：老子有实力','2026-10-11 20:49:59','2026-10-11 20:49:59'),
(24,9,NULL,14,'2026-10-15 13:17:03','领养：试试看','2026-10-15 13:17:03','2026-10-15 13:17:03');
/*!40000 ALTER TABLE `pet_transfer` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `prescription`
--

DROP TABLE IF EXISTS `prescription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `prescription` (
  `prescription_id` int(11) NOT NULL AUTO_INCREMENT,
  `record_id` int(11) NOT NULL COMMENT '病历id',
  `drug_id` int(11) NOT NULL,
  `drug_name` varchar(64) DEFAULT NULL COMMENT '冗余:开方时名称',
  `drug_spec` varchar(64) DEFAULT NULL,
  `drug_unit` varchar(16) DEFAULT NULL,
  `drug_price` decimal(10,2) DEFAULT 0.00 COMMENT '冗余:开方时单价',
  `quantity` int(11) NOT NULL DEFAULT 1,
  `daily_times` int(11) DEFAULT NULL COMMENT 'é¢‘æ¬¡ï¼šæ¯æ—¥å‡ æ¬¡',
  `single_dose` decimal(10,3) DEFAULT NULL COMMENT 'å•æ¬¡å‰‚é‡(å•ä½åŒè¯å‰‚å•ä½)',
  `single_dose_unit` varchar(16) DEFAULT NULL COMMENT 'å•æ¬¡å‰‚é‡å•ä½(ç‰‡/mL/è¢‹ç­‰)',
  `usage_info` varchar(128) DEFAULT NULL COMMENT '用法,如每日2次每次1片',
  `is_del` bigint(20) DEFAULT 0,
  `create_time` datetime DEFAULT current_timestamp(),
  `update_time` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `is_cancel` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'æ˜¯å¦ä½œåºŸ',
  PRIMARY KEY (`prescription_id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='处方明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prescription`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `prescription` WRITE;
/*!40000 ALTER TABLE `prescription` DISABLE KEYS */;
INSERT INTO `prescription` VALUES
(1,1,2,'头孢氨苄片','0.25g×24片','盒',15.50,1,NULL,NULL,NULL,'',0,'2026-10-12 11:33:46','2026-10-12 11:33:46',0),
(2,1,8,'眼药水','8ml','瓶',26.00,2,NULL,NULL,NULL,'',0,'2026-10-12 11:33:46','2026-10-12 11:33:46',0),
(17,15,8,'眼药水','8ml','瓶',26.00,1,NULL,NULL,NULL,'',0,'2026-10-13 20:04:27','2026-10-13 20:04:27',0),
(18,15,5,'驱虫滴剂(猫)','1ml×3支','盒',68.00,1,NULL,NULL,NULL,'',0,'2026-10-13 20:04:27','2026-10-13 20:04:27',0),
(19,16,8,'眼药水','8ml','瓶',26.00,1,NULL,NULL,NULL,'',0,'2026-10-14 11:23:11','2026-10-14 11:23:11',0),
(20,17,2,'头孢氨苄片','0.25g×24片','盒',15.50,1,NULL,NULL,NULL,'',0,'2026-10-14 16:57:12','2026-10-14 16:57:12',0),
(21,18,2,'头孢氨苄片','0.25g×24片','盒',15.50,1,NULL,NULL,NULL,'',0,'2026-10-17 11:17:18','2026-10-17 11:17:18',0),
(22,18,6,'益生菌粉','5g×10袋','盒',39.00,1,NULL,NULL,NULL,'',0,'2026-10-17 11:17:18','2026-10-17 11:17:18',0),
(23,19,8,'眼药水','8ml','瓶',26.00,1,2,NULL,NULL,'',0,'2026-10-22 15:50:09','2026-10-22 15:50:09',0),
(24,19,6,'益生菌粉','5g×10袋','盒',39.00,1,3,NULL,NULL,'',0,'2026-10-22 15:50:09','2026-10-22 15:50:09',0);
/*!40000 ALTER TABLE `prescription` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `record_package`
--

DROP TABLE IF EXISTS `record_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `record_package` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `record_id` int(11) NOT NULL COMMENT '病历id',
  `package_id` int(11) DEFAULT NULL COMMENT '套餐id',
  `package_name` varchar(64) DEFAULT NULL COMMENT '冗余套餐名',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '成交价',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT 'ä¿®æ”¹æ—¶é—´',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='就诊套餐记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `record_package`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `record_package` WRITE;
/*!40000 ALTER TABLE `record_package` DISABLE KEYS */;
INSERT INTO `record_package` VALUES
(2,15,2,'全面体检套餐',299.00,'2026-10-13 20:04:27','2026-10-13 20:04:27',0),
(3,18,1,'基础体检套餐',99.00,'2026-10-17 11:17:35','2026-10-17 11:17:35',0),
(4,19,1,'基础体检套餐',99.00,'2026-10-22 15:51:14','2026-10-22 15:51:14',0);
/*!40000 ALTER TABLE `record_package` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `record_template`
--

DROP TABLE IF EXISTS `record_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `record_template` (
  `template_id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT 'æ¨¡æ¿å',
  `category` varchar(32) DEFAULT NULL COMMENT 'åˆ†ç±»ï¼šå¸¸è§ç—…/ä½“æ£€/æœ¯åŽ/å¤è¯Š',
  `chief_complaint` varchar(500) DEFAULT NULL,
  `present_illness` text DEFAULT NULL COMMENT 'çŽ°ç—…å²æ¨¡æ¿',
  `past_history` text DEFAULT NULL COMMENT 'æ—¢å¾€ç—…å²æ¨¡æ¿',
  `physical_exam` varchar(500) DEFAULT NULL,
  `diagnosis` varchar(500) DEFAULT NULL,
  `treatment` varchar(500) DEFAULT NULL,
  `advice` varchar(500) DEFAULT NULL,
  `internal_note` varchar(500) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_del` bigint(20) DEFAULT 0,
  PRIMARY KEY (`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='ç”µå­ç—…åŽ†æ¨¡æ¿';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `record_template`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `record_template` WRITE;
/*!40000 ALTER TABLE `record_template` DISABLE KEYS */;
INSERT INTO `record_template` VALUES
(1,'犬瘟热初诊','常见病','精神沉郁、食欲废绝、发热、鼻镜干裂',NULL,NULL,NULL,'疑似犬瘟热（需试纸/核酸确诊）','补液、抗病毒、对症支持、隔离','告知预后与传染性，建议住院隔离治疗',NULL,1,NULL,NULL,0),
(2,'年度体检','体检','主人主诉无异常，年度健康检查',NULL,NULL,NULL,'待检查','血常规+生化+影像','禁食8小时后采血',NULL,1,NULL,NULL,0),
(3,'绝育术后','术后','已行绝育手术，术后复查',NULL,NULL,NULL,'绝育术后恢复期','复查伤口、镇痛、抗生素','佩戴伊丽莎白圈，7天拆线',NULL,1,NULL,NULL,0);
/*!40000 ALTER TABLE `record_template` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `rx_print_supplement`
--

DROP TABLE IF EXISTS `rx_print_supplement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `rx_print_supplement` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `prescription_id` bigint(20) NOT NULL COMMENT 'å¤„æ–¹è¡ŒID(å¤šå¤„æ–¹æ—¶å®šä½åˆ°å…·ä½“è¯å“è¡Œ;æ•´å•æ‰“å°å–è¯¥ç—…åŽ†ä»»ä¸€å¤„æ–¹è¡Œï¼Œç”±æœåŠ¡å±‚å›ºå®šè§„åˆ™)',
  `record_id` bigint(20) NOT NULL COMMENT 'ç—…åŽ†ID',
  `print_no` varchar(32) NOT NULL COMMENT 'å¤„æ–¹ç¬ºç¼–å· RX-YYYYMMDD-XXXX(æŒ‰å¤©è‡ªå¢žï¼Œä½œåºŸå å·ä¸å›žæ”¶)',
  `hair_color` varchar(16) DEFAULT NULL COMMENT 'æ¯›è‰²(ä¼˜å…ˆpet.pet_colorï¼Œå–ä¸åˆ°æ—¶è¡¥å½•ï¼Œä¸å›žå†™pet)',
  `species` varchar(16) DEFAULT NULL COMMENT 'åŠ¨ç‰©ç§ç±»(çŠ¬/çŒ«/å…”â€¦ï¼Œpet_varietyæ˜¯å“ç§éžç§ç±»ï¼Œç¼ºå¤±æ—¶è¡¥å½•)',
  `withdrawal_period` varchar(32) DEFAULT NULL COMMENT 'ä¼‘è¯æœŸ(å® ç‰©å¤„æ–¹å›ºå®š"â€”"ï¼Œé£Ÿå“åŠ¨ç‰©ä»Žè¯å“å–ï¼Œå–ä¸åˆ°è¡¥å½•)',
  `dispenser` varchar(50) DEFAULT NULL COMMENT 'å‘è¯äºº(ä¼˜å…ˆbill.dispense_byå§“åï¼Œæœªå‘è¯æ—¶è¡¥å½•æˆ–ç•™ç©ºæ‰‹å†™)',
  `copies` tinyint(4) NOT NULL DEFAULT 1 COMMENT 'è”æ¬¡ä»½æ•° 1=ä»…ç¬¬ä¸‰è”(ç•œä¸»ç•™å­˜) 3=ä¸‰è”å…¨æ‰“',
  `status` varchar(8) NOT NULL DEFAULT 'VALID' COMMENT 'VALIDæœ‰æ•ˆ / VOIDå·²ä½œåºŸ',
  `printed_by` bigint(20) DEFAULT NULL COMMENT 'æ‰“å°è§¦å‘äººID',
  `printed_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT 'æœ€è¿‘æ‰“å°æ—¶é—´(ç•™ç—•ï¼Œä¸å°å¤„æ–¹ç¬ºæ­£æ–‡)',
  `void_by` bigint(20) DEFAULT NULL COMMENT 'ä½œåºŸæ“ä½œäºº',
  `void_at` datetime DEFAULT NULL COMMENT 'ä½œåºŸæ—¶é—´',
  `void_reason` varchar(255) DEFAULT NULL COMMENT 'ä½œåºŸåŽŸå› ',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_no` (`print_no`),
  UNIQUE KEY `uk_prescription` (`prescription_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_printed_at` (`printed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='å…½ç”¨å¤„æ–¹ç¬ºæ‰“å°è¡¥å……+ç•™ç—•(æ­£æ–‡ä»ä»¥å¤„æ–¹è¡¨ä¸ºå‡†)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rx_print_supplement`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `rx_print_supplement` WRITE;
/*!40000 ALTER TABLE `rx_print_supplement` DISABLE KEYS */;
/*!40000 ALTER TABLE `rx_print_supplement` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `service_package`
--

DROP TABLE IF EXISTS `service_package`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `service_package` (
  `package_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '套餐id',
  `package_name` varchar(64) NOT NULL COMMENT '套餐名',
  `category` varchar(32) DEFAULT NULL COMMENT '分类：驱虫/体检/免疫/美容等',
  `price` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '套餐价(元)',
  `content` varchar(255) DEFAULT NULL COMMENT '包含内容(文本描述)',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '启停',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`package_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='服务套餐字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_package`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `service_package` WRITE;
/*!40000 ALTER TABLE `service_package` DISABLE KEYS */;
INSERT INTO `service_package` VALUES
(1,'基础体检套餐','体检',99.00,'体格检查+血常规+体温心率',1,NULL,'2026-10-20 18:23:21',0),
(2,'全面体检套餐','体检',299.00,'体格检查+血常规+生化+影像',1,NULL,NULL,0),
(3,'犬用驱虫套餐','驱虫',80.00,'体内外驱虫+复查',1,NULL,NULL,0),
(4,'猫用驱虫套餐','驱虫',80.00,'体内外驱虫+复查',1,NULL,NULL,0),
(5,'幼宠免疫套餐','免疫',200.00,'首免疫苗+体检+驱虫',1,NULL,NULL,0);
/*!40000 ALTER TABLE `service_package` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `supplier`
--

DROP TABLE IF EXISTS `supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier` (
  `supplier_id` int(11) NOT NULL AUTO_INCREMENT,
  `supplier_name` varchar(128) NOT NULL COMMENT 'ä¾›åº”å•†åç§°',
  `contact` varchar(64) DEFAULT NULL COMMENT 'è”ç³»äºº',
  `phone` varchar(32) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `is_del` bigint(20) DEFAULT 0,
  PRIMARY KEY (`supplier_id`),
  KEY `idx_name` (`supplier_name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='ä¾›åº”å•†';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `supplier` WRITE;
/*!40000 ALTER TABLE `supplier` DISABLE KEYS */;
INSERT INTO `supplier` VALUES
(1,'河北康宠商贸有限公司','张总','0371-1234567','石家庄','',1,'2026-10-16 19:12:44','2026-10-16 19:12:44',0),
(2,'维龙商贸','王宏彬','','','',1,'2026-10-20 15:48:57','2026-10-20 15:48:57',0);
/*!40000 ALTER TABLE `supplier` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `surgery`
--

DROP TABLE IF EXISTS `surgery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `surgery` (
  `surgery_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '手术单id',
  `record_id` int(11) NOT NULL COMMENT '病历id',
  `pet_id` int(11) NOT NULL COMMENT '宠物id',
  `client_id` int(11) NOT NULL COMMENT '客户id',
  `doctor_id` int(11) DEFAULT NULL COMMENT '主刀医生id',
  `surgery_name` varchar(128) NOT NULL COMMENT '手术名称',
  `surgery_type` varchar(32) DEFAULT NULL COMMENT '类型：绝育/骨科/肿瘤/外伤/其他',
  `anesthesia_type` varchar(32) DEFAULT NULL COMMENT '麻醉方式：全身麻醉/局部麻醉/镇静',
  `scheduled_time` datetime DEFAULT NULL COMMENT '预约手术时间',
  `start_time` datetime DEFAULT NULL COMMENT '实际开始',
  `end_time` datetime DEFAULT NULL COMMENT '实际结束',
  `surgeon` varchar(64) DEFAULT NULL COMMENT '主刀',
  `assistant` varchar(64) DEFAULT NULL COMMENT '助手',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0待手术 1手术中 2已完成 3已取消',
  `preop_note` varchar(500) DEFAULT NULL COMMENT '术前评估/准备',
  `intraop_note` varchar(1000) DEFAULT NULL COMMENT '术中记录',
  `postop_note` varchar(1000) DEFAULT NULL COMMENT '术后记录/医嘱',
  `fee` decimal(10,2) NOT NULL DEFAULT 0.00 COMMENT '手术费(元)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`surgery_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_client` (`client_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_uca1400_ai_ci COMMENT='手术单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `surgery`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `surgery` WRITE;
/*!40000 ALTER TABLE `surgery` DISABLE KEYS */;
INSERT INTO `surgery` VALUES
(1,12,37,1,10,'公猫绝育','绝育','全身麻醉',NULL,NULL,NULL,'','',0,NULL,NULL,NULL,2000.00,'2026-10-13 21:06:13','2026-10-20 20:37:41',0),
(2,19,36,1,10,'母猫绝育','绝育','全身麻醉',NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,666.00,'2026-10-22 15:51:04','2026-10-22 15:51:04',0);
/*!40000 ALTER TABLE `surgery` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `surgery_consent`
--

DROP TABLE IF EXISTS `surgery_consent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `surgery_consent` (
  `consent_id` int(11) NOT NULL AUTO_INCREMENT,
  `surgery_id` int(11) NOT NULL COMMENT 'æ‰‹æœ¯å•id',
  `record_id` int(11) DEFAULT NULL,
  `pet_id` int(11) DEFAULT NULL,
  `client_id` int(11) DEFAULT NULL,
  `surgery_name` varchar(128) DEFAULT NULL COMMENT 'å†—ä½™æ‰‹æœ¯å',
  `anesthesia_type` varchar(64) DEFAULT NULL COMMENT 'å†—ä½™éº»é†‰æ–¹å¼',
  `risks` varchar(1000) DEFAULT NULL COMMENT 'é£Žé™©ä¸Žæ³¨æ„äº‹é¡¹å‘ŠçŸ¥',
  `agree` tinyint(1) DEFAULT NULL COMMENT 'ç•œä¸»æ˜¯å¦åŒæ„',
  `signer` varchar(64) DEFAULT NULL COMMENT 'ç­¾å­—äºº(ç•œä¸»å§“å)',
  `signed_time` datetime DEFAULT NULL COMMENT 'ç­¾ç½²æ—¶é—´',
  `operator` varchar(50) DEFAULT NULL COMMENT 'ç»åŠžåŒ»æŠ¤',
  `note` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`consent_id`),
  UNIQUE KEY `uk_surgery` (`surgery_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='æ‰‹æœ¯/éº»é†‰çŸ¥æƒ…åŒæ„ä¹¦';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `surgery_consent`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `surgery_consent` WRITE;
/*!40000 ALTER TABLE `surgery_consent` DISABLE KEYS */;
/*!40000 ALTER TABLE `surgery_consent` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Table structure for table `sys_dict`
--

DROP TABLE IF EXISTS `sys_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict` (
  `dict_id` int(11) NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(32) NOT NULL COMMENT 'å­—å…¸ç±»åž‹ lab_group/img_group/drug_type/drug_unit/surgery_type/anesthesia_type',
  `dict_key` varchar(64) NOT NULL COMMENT 'å­—å…¸å€¼',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT 'æŽ’åº',
  `active` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'å¯åœ',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`dict_id`),
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci COMMENT='é€šç”¨å­—å…¸/ç³»ç»Ÿè®¾ç½®';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict`
--

SET @OLD_AUTOCOMMIT=@@AUTOCOMMIT, @@AUTOCOMMIT=0;
LOCK TABLES `sys_dict` WRITE;
/*!40000 ALTER TABLE `sys_dict` DISABLE KEYS */;
INSERT INTO `sys_dict` VALUES
(1,'drug_unit','盒',1,1,NULL),
(2,'drug_unit','瓶',2,1,NULL),
(3,'drug_unit','支',3,1,NULL),
(4,'drug_unit','袋',4,1,NULL),
(5,'drug_unit','毫升',5,1,NULL),
(6,'drug_unit','片',6,1,NULL),
(7,'drug_type','西药',1,1,NULL),
(8,'drug_type','中成药',2,1,NULL),
(9,'drug_type','外用',3,1,NULL),
(10,'drug_type','疫苗',4,1,NULL),
(11,'drug_type','驱虫',5,1,NULL),
(12,'drug_type','保健',6,1,NULL),
(13,'lab_group','血常规',1,1,NULL),
(14,'lab_group','生化',2,1,NULL),
(15,'lab_group','尿检',3,1,NULL),
(16,'lab_group','镜检',4,1,NULL),
(17,'img_group','X光',1,1,NULL),
(18,'img_group','B超',2,1,NULL),
(19,'img_group','CT',3,1,NULL),
(20,'surgery_type','绝育',1,1,NULL),
(21,'surgery_type','骨科',2,1,NULL),
(22,'surgery_type','肿瘤',3,1,NULL),
(23,'surgery_type','外伤',4,1,NULL),
(24,'surgery_type','其他',5,1,NULL),
(25,'anesthesia_type','全身麻醉',1,1,NULL),
(26,'anesthesia_type','局部麻醉',2,1,NULL),
(27,'anesthesia_type','镇静',3,1,NULL);
/*!40000 ALTER TABLE `sys_dict` ENABLE KEYS */;
UNLOCK TABLES;
COMMIT;
SET AUTOCOMMIT=@OLD_AUTOCOMMIT;

--
-- Dumping routines for database 'pet_mix'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*M!100616 SET NOTE_VERBOSITY=@OLD_NOTE_VERBOSITY */;

-- P0 资金一致性（2026-11-07）：独立收款时间 + 累计退款额。
-- 放在所有 INSERT 之后追加列，避免影响不带列名的 INSERT INTO `bill` VALUES。
ALTER TABLE `bill`
  ADD COLUMN `pay_time` datetime DEFAULT NULL COMMENT '收款时间（独立于 update_time）',
  ADD COLUMN `refund_money` decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计已退金额（实付口径）';
UPDATE `bill` SET `pay_time` = `update_time` WHERE `pay_status` = 1 AND `pay_time` IS NULL;

-- 审计日志表随基线一起创建，避免只导入基线时审计写入静默失败
CREATE TABLE IF NOT EXISTS `audit_log` (
    `log_id`      INT AUTO_INCREMENT COMMENT '日志id',
    `operator`    VARCHAR(64)  NOT NULL COMMENT '操作人（登录态取，防伪造）',
    `role`        VARCHAR(32)  DEFAULT NULL COMMENT '操作角色',
    `module`      VARCHAR(32)  NOT NULL COMMENT '模块',
    `action`      VARCHAR(64)  NOT NULL COMMENT '动作',
    `target`      VARCHAR(128) DEFAULT NULL COMMENT '操作对象',
    `detail`      VARCHAR(512) DEFAULT NULL COMMENT '详情（含成功/失败状态）',
    `ip`          VARCHAR(64)  DEFAULT NULL COMMENT '来源IP（直连地址）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_operator` (`operator`),
    KEY `idx_module_action` (`module`, `action`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB COMMENT = '操作审计日志';

-- P1 号源（2026-11-07）：医生 + 就诊日期复合索引，配合 reserve() 的 FOR UPDATE 行锁，
-- 让整日/时段计数走索引，避免退化成全表扫描。
ALTER TABLE `appointment` ADD INDEX `idx_doctor_date` (`doctor_id`, `appointment_date`);

-- P3 临床合规与闭环（2026-11-07）：与 db-backup/feature_20261107_p3_clinical.sql 等价，新库直接执行

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

-- P4 临床专业度深化（2026-11-07）：与 db-backup/feature_20261107_p4_clinical.sql 等价，新库直接执行

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

-- Dump completed on 2026-10-24 18:42:10
