-- P0-3 微信支付/公众号骨架（2026-11-13）：商户参数留配置位，enabled=false 时全链路旁路。
-- 1) 客户 openid 绑定列：公众号 OAuth 网页授权回调后写入，JSAPI 支付与模板消息都依赖它
-- 2) 账单微信流水号列：微信支付回调成功后落库，与微信商户平台对账
-- 适用于：已存在运行库（旧库）升级。全新部署直接导入 pet_mix.sql 即可（下次重压基线时并入）。
-- 可重复执行：information_schema 判存在，重复执行跳过。

-- 1) client 表：openid
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client' AND COLUMN_NAME = 'client_openid');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `client` ADD COLUMN `client_openid` VARCHAR(64) DEFAULT NULL COMMENT ''微信openid（公众号OAuth绑定，JSAPI支付与模板消息用）'' AFTER `client_info`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- openid 建索引（按 openid 反查客户）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'client' AND INDEX_NAME = 'idx_client_openid');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE `client` ADD INDEX `idx_client_openid` (`client_openid`)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) bill 表：微信商户单号 + 交易号（预下单落单号，回调落交易号；查单/退款/对账都要用）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bill' AND COLUMN_NAME = 'wx_out_trade_no');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `bill` ADD COLUMN `wx_out_trade_no` VARCHAR(64) DEFAULT NULL COMMENT ''微信商户单号（预下单落库，查单/退款用）'' AFTER `pay_by`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bill' AND COLUMN_NAME = 'wx_transaction_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `bill` ADD COLUMN `wx_transaction_id` VARCHAR(64) DEFAULT NULL COMMENT ''微信支付交易号（回调成功落库，对账用）'' AFTER `wx_out_trade_no`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
