-- 高频查询字段索引补齐（数据量上来前的预防性优化）
-- 背景：全面检查报告的建议，经 information_schema.STATISTICS 核对后仅以下三处真实缺失：
--   ① bill.record_id            —— 病历查账单/处方作废冲减/免疫计费均按 record_id 定位（当前全表扫描）
--   ② bill.pay_status           —— 收银台按 已付/未付 筛选（与 record_id 建联合索引覆盖「查某病历未付账单」热点）
--   ③ client.client_tel         —— 登录/挂号/充值按手机号定位客户（client_username 已有联合索引但登录走 tel）
--   ④ medical_record.doctor_id  —— 医生端「我接诊的病例」按 doctor_id 筛选
-- 幂等处理：先查 information_schema，已存在则跳过（避免重复执行报错）

SET @db := DATABASE();

-- bill: record_id + pay_status 联合索引（左前缀可独立服务 record_id 单列查询）
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'bill' AND INDEX_NAME = 'idx_record_pay') > 0,
    'SELECT 1',
    'ALTER TABLE `bill` ADD INDEX `idx_record_pay` (`record_id`, `pay_status`)'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- client: client_tel 独立索引（登录/挂号/充值/ExistsByTel）
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'client' AND INDEX_NAME = 'idx_tel') > 0,
    'SELECT 1',
    'ALTER TABLE `client` ADD INDEX `idx_tel` (`client_tel`)'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- medical_record: doctor_id 索引（医生端病例筛选）
SET @s := (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'medical_record' AND INDEX_NAME = 'idx_doctor') > 0,
    'SELECT 1',
    'ALTER TABLE `medical_record` ADD INDEX `idx_doctor` (`doctor_id`)'
));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
