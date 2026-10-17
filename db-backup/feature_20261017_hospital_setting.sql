-- 医院基础信息（单行配置 id=1）：名称/地址/电话/logo/营业时间/简介
-- GET /setting/hospital 公开（打印页/顾客端读取）；PUT 仅 MANAGER
-- 适用数据库：pet_ship

CREATE TABLE IF NOT EXISTS `hospital_setting` (
  `id`         TINYINT      NOT NULL DEFAULT 1,
  `name`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '医院名称',
  `address`    VARCHAR(160) NOT NULL DEFAULT '' COMMENT '地址',
  `phone`      VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '联系电话',
  `logo`       VARCHAR(255) DEFAULT NULL COMMENT 'logo文件名(复用common/download)',
  `biz_hours`  VARCHAR(64)  DEFAULT '' COMMENT '营业时间',
  `intro`      VARCHAR(255) DEFAULT '' COMMENT '简介/宣传文案',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医院基础信息(单行配置)';

INSERT INTO `hospital_setting` (`id`) VALUES (1) ON DUPLICATE KEY UPDATE `id` = `id`;
