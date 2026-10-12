-- =============================================
-- 医生 ⇄ 科室 多对多关系表（替代医生单选主科室）
-- 执行方式：mysql -u mixpet -p pet_ship < 数据库备份/doctor_department.sql
-- =============================================

-- 1. 建表：一个医生可挂多个科室，一个科室可有多个医生
CREATE TABLE IF NOT EXISTS `doctor_department` (
  `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `doctor_id` INT NOT NULL COMMENT '医生id',
  `department_id` INT NOT NULL COMMENT '科室id',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_doctor_depart` (`doctor_id`, `department_id`)
) COMMENT='医生可执业科室（多对多）';

-- 2. 迁移历史数据：把现有医生的主科室写入关系表（每个医生至少一条）
INSERT IGNORE INTO `doctor_department` (doctor_id, department_id)
SELECT doctor_id, department_id FROM doctor WHERE is_del = 0 AND department_id IS NOT NULL;

-- 3. 校验：每个有主科室的医生都已进入关系表
-- SELECT doctor_id, GROUP_CONCAT(department_id) FROM doctor_department GROUP BY doctor_id;