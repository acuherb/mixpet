-- 顾客注册审核：client 增加审核状态
-- 适用数据库：pet_ship
-- 说明：
--   audit_status 审核状态：0待审核 1通过 2拒绝
--   自助注册顾客默认 0(待审核)，需管理员在后台通过后才能登录；
--   历史顾客与后台手动新增的顾客默认为 1(通过)，不受影响

ALTER TABLE `client`
    ADD COLUMN `audit_status` tinyint NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1通过 2拒绝' AFTER `active`;

-- 历史顾客默认通过
UPDATE `client` SET `audit_status` = 1 WHERE `audit_status` = 0;
