-- 影像项目增加报告模板字段：录报告时按预置项结构化填写，医生按规范填空
-- 适用数据库：pet_mix

ALTER TABLE `image_test`
    ADD COLUMN `report_tpl` text NULL COMMENT '报告模板项(JSON数组字符串，如 ["检查体位","影像所见","影像诊断"])' AFTER `detail`;
