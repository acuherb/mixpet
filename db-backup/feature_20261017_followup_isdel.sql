-- 随访表补 is_del 逻辑删除列：与 lab_order/image_order/surgery 等表保持一致
-- 背景：MyBatis-Plus 全局逻辑删除字段为 is_del，follow_up 缺此列会导致
--       followUpMapper 的 lambda 查询自动拼 is_del=0 而报 Unknown column
-- 适用数据库：pet_ship

ALTER TABLE `follow_up`
    ADD COLUMN `is_del` bigint NULL DEFAULT NULL COMMENT '逻辑删除，默认0，填充删除日期' AFTER `create_time`;

-- 存量数据视为未删除
UPDATE `follow_up` SET `is_del` = 0 WHERE `is_del` IS NULL;
