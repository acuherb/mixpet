-- 顾客注册审核：增加拒绝原因字段，审核拒绝时记录原因，登录拦截时带出给顾客
-- 适用数据库：pet_mix

ALTER TABLE `client`
    ADD COLUMN `audit_reason` varchar(255) NULL DEFAULT NULL COMMENT '注册审核拒绝原因(登录时提示顾客)' AFTER `audit_status`;
