-- 聊天已读/未读：为 msg 增加接收方已读标记
-- 适用数据库：pet_ship
-- 说明：
--   is_read    接收方是否已读：0未读 1已读
--             （顾客发的消息由医生读取；医生发的消息由顾客读取）
--   read_time  已读时间
--   历史消息统一置为已读，避免一上线全是未读红点

ALTER TABLE `msg`
    ADD COLUMN `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '接收方是否已读：0未读 1已读' AFTER `msg_name`,
    ADD COLUMN `read_time` datetime DEFAULT NULL COMMENT '已读时间' AFTER `is_read`;

-- 历史数据默认已读（只影响本次升级前的存量消息）
UPDATE `msg` SET `is_read` = 1 WHERE `is_read` = 0;
