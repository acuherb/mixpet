-- P1 号源与排班（2026-11-07）
-- 适用于已有运行库升级；全新部署直接导入 pet_mix.sql 即可（基线已含同样 DDL）。
-- 可重复执行。

-- 1) 排班逻辑删除列默认值：原 DEFAULT NULL 与全局逻辑删除条件 is_del=0 冲突，
--    导致保存后的排班查不出来、max_count 永远不生效。
ALTER TABLE `doctor_schedule`
  MODIFY COLUMN `is_del` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删，删除填时间戳';
UPDATE `doctor_schedule` SET `is_del` = 0 WHERE `is_del` IS NULL;

-- 2) 号源计数复合索引（幂等）：挂号/改期的行锁事务内按医生+日期范围计数
SET @ddl := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `appointment` ADD INDEX `idx_doctor_date` (`doctor_id`, `appointment_date`)',
              'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'appointment' AND INDEX_NAME = 'idx_doctor_date');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
