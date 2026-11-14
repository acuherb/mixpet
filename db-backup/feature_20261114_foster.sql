-- 寄养闭环补齐（2026-11-14）
-- 背景：原 foster 只有「押金 foster_money + 三项日单价 + status」，接回时 back() 把 foster_money
--       覆盖成结算金额（押金丢失、不冲抵）、费用记进 fee_package（顾客看到「套餐费」）、
--       账单 record_id=null 无 foster_id 回指；且 updateFoster 不更新 status/in_date/单价，
--       导致「开始寄养」状态写不进库，流程卡死。另无每日照护记录/照片表。
-- 本脚本：① foster 补结算明细列（天数/定价/冲抵/应退/净额/改价留痕/账单回指）
--        ② bill 补 fee_foster 与 foster_id，让寄养费独立分项且可追溯
--        ③ 新建 foster_log（每日文字日志）与 foster_photo（每日照片）
-- 幂等：用 information_schema 判断列是否存在 + CREATE TABLE IF NOT EXISTS，可重复执行。

USE pet_mix;

-- ① foster 结算明细列
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'foster' AND COLUMN_NAME = 'settle_money');
SET @sql = IF(@s = 0,
  "ALTER TABLE foster
     ADD COLUMN day_count INT(11) DEFAULT NULL COMMENT '实际寄养天数（结算时落库）',
     ADD COLUMN settle_money DECIMAL(10,2) DEFAULT NULL COMMENT '最终结算定价（含人工改价）',
     ADD COLUMN deposit_used DECIMAL(10,2) DEFAULT NULL COMMENT '押金冲抵金额',
     ADD COLUMN refund_money DECIMAL(10,2) DEFAULT NULL COMMENT '应退金额（押金>定价时）',
     ADD COLUMN charge_money DECIMAL(10,2) DEFAULT NULL COMMENT '实际计入账单的净额',
     ADD COLUMN adjust_note VARCHAR(255) DEFAULT NULL COMMENT '人工改价说明（留痕）',
     ADD COLUMN bill_id INT(11) DEFAULT NULL COMMENT '接回结算生成的账单id'",
  "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ② bill 寄养分项与寄养单回指
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bill' AND COLUMN_NAME = 'fee_foster');
SET @sql = IF(@s = 0,
  "ALTER TABLE bill
     ADD COLUMN fee_foster DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '寄养费',
     ADD COLUMN foster_id INT(11) DEFAULT NULL COMMENT '关联寄养单（可追溯对账）',
     ADD KEY idx_bill_foster (foster_id)",
  "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③ 每日照护记录（文字）
CREATE TABLE IF NOT EXISTS foster_log (
  log_id       INT(11) NOT NULL AUTO_INCREMENT,
  foster_id    INT(11) NOT NULL COMMENT '寄养单id',
  log_date     DATE NOT NULL COMMENT '照护日期',
  mental_state VARCHAR(32) DEFAULT NULL COMMENT '精神状态',
  appetite     VARCHAR(32) DEFAULT NULL COMMENT '食欲',
  defecation   VARCHAR(32) DEFAULT NULL COMMENT '排便',
  weight       DECIMAL(8,2) DEFAULT NULL COMMENT '当日体重kg',
  note         VARCHAR(500) DEFAULT NULL COMMENT '照护备注/给主人留言',
  recorder     VARCHAR(64) DEFAULT NULL COMMENT '记录人',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_del       BIGINT(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (log_id),
  KEY idx_fl_foster (foster_id),
  KEY idx_fl_date (log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寄养每日照护记录';

-- ④ 每日照片
CREATE TABLE IF NOT EXISTS foster_photo (
  photo_id     INT(11) NOT NULL AUTO_INCREMENT,
  foster_id    INT(11) NOT NULL COMMENT '寄养单id',
  log_id       INT(11) DEFAULT NULL COMMENT '关联当日日志（可为空：单独补图）',
  log_date     DATE NOT NULL COMMENT '拍摄日期',
  file_name    VARCHAR(128) NOT NULL COMMENT '上传后文件名（UUID）',
  origin_name  VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_del       BIGINT(20) NOT NULL DEFAULT 0,
  PRIMARY KEY (photo_id),
  KEY idx_fp_foster (foster_id),
  KEY idx_fp_log (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寄养每日照片';

-- ⑤ 通知类型无需 DDL：client_notification.notify_type 为 VARCHAR(32)，
--    新增 FOSTER_IN / FOSTER_LOG / FOSTER_BACK / FOSTER_BILL 由业务层写入。
