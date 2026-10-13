-- ============================================================
-- 医院职务/职称字典（与权限等级 auth_lv 解耦，仅作展示）
-- 适用：MySQL 8 / MariaDB
-- 说明：权限仍是固定的 auth_lv(0/2/4/6/8) 驱动角色；职称另建字典可增删改
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 职务/职称字典
-- ----------------------------
DROP TABLE IF EXISTS `job`;
CREATE TABLE `job` (
  `job_id` int NOT NULL AUTO_INCREMENT COMMENT '职称id',
  `job_name` varchar(32) NOT NULL COMMENT '职称名称',
  `sort` int NOT NULL DEFAULT '99' COMMENT '排序，越小越靠前',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  `is_del` bigint NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb3 COMMENT='医院职务/职称字典';

-- ----------------------------
-- 2. 医生表新增职称外键
-- ----------------------------
ALTER TABLE `doctor`
  ADD COLUMN `job_id` int DEFAULT NULL COMMENT '职称id(关联job)' AFTER `auth_lv`;

-- ----------------------------
-- 3. 预置常用职称
-- ----------------------------
INSERT INTO `job` (`job_name`, `sort`) VALUES
('院长', 1),
('副院长', 2),
('科室主任', 3),
('副主任医师', 4),
('主治医师', 5),
('医师', 6),
('实习医师', 7),
('护士长', 8),
('护理技师', 9),
('营业员', 10);

-- ----------------------------
-- 4. 给现有医生按权限等级播种初始职称（保持原显示不回退）
-- ----------------------------
UPDATE doctor d
  JOIN job j ON j.job_name = (CASE d.auth_lv
      WHEN 8 THEN '院长'
      WHEN 6 THEN '科室主任'
      WHEN 4 THEN '主治医师'
      WHEN 2 THEN '护理技师'
      ELSE '实习医师' END)
SET d.job_id = j.job_id
WHERE d.is_del = 0 AND d.job_id IS NULL;
