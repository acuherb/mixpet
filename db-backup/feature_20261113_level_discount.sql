-- P0-6 灵活折扣：会员等级按商品类别(drug_type)配置折扣率（2026-11-13）
-- 背景：PetSystem ecmmemberdc2 是「储值套餐按商品前缀(PREFIX)折扣」的灵活折扣，用户拍板现代化为
--       「等级折扣 × 类别折扣」：类别折扣在开方时折进处方单价(prescription.drug_price)，
--       bill.discount 仍只放等级折扣，real_money 生成列/退款/报表逻辑零改动。
-- 适用于：已存在运行库（旧库）升级。全新部署直接导入 pet_mix.sql 即可（下次重压基线时并入）。
-- 可重复执行：表已存在时自动跳过。

CREATE TABLE IF NOT EXISTS `member_level_discount` (
    `id`         INT AUTO_INCREMENT COMMENT '主键',
    `level_id`   INT           NOT NULL COMMENT '会员等级id(member_level.level_id)',
    `item_type`  VARCHAR(16)   NOT NULL COMMENT '商品类别(=sys_dict dict_type=drug_type 的值，如 用品/保健/疫苗)',
    `discount`   DECIMAL(4,3)  NOT NULL DEFAULT 1.000 COMMENT '类别折扣率(0-1，0.9=九折；与等级折扣叠加)',
    `active`     TINYINT(1)    NOT NULL DEFAULT 1 COMMENT '启停',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_type` (`level_id`, `item_type`)
) ENGINE = InnoDB COMMENT = '会员等级类别折扣（开方单价折算用）';
