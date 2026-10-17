-- 品牌主题色可配置（E15）：hospital_setting 增加 brand_color，配合 CSS 变量全局注入
-- 适用数据库：pet_ship

ALTER TABLE `hospital_setting`
    ADD COLUMN `brand_color` varchar(16) NOT NULL DEFAULT '#1E6FA8' COMMENT '品牌主题色(HEX，默认蓝)' AFTER `logo`;
