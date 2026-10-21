-- 聊天增强：发送文件 + 图片转 WebP + 目录结构调整（chat/image、chat/file）
-- 适用数据库：pet_mix
-- 说明：
--   1. msg_type  TEXT 文字 / IMAGE 图片 / FILE 文件
--   2. msg_name  文件消息的原始文件名（仅 FILE 类型使用）
--   3. 历史 is_img=1 的图片消息自动补为 IMAGE 类型

ALTER TABLE `msg`
    ADD COLUMN `msg_type` varchar(10) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/IMAGE/FILE' AFTER `is_img`,
    ADD COLUMN `msg_name` varchar(255) DEFAULT NULL COMMENT '文件消息的原始文件名' AFTER `msg_type`;

-- 历史图片消息标记为 IMAGE 类型
UPDATE `msg` SET `msg_type` = 'IMAGE' WHERE `is_img` = 1 AND `msg_type` = 'TEXT';