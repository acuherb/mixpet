-- 病历结构化补充：现病史(症状发展过程) + 既往病史(过去重要医疗信息)
-- 依据小弗规范病历教程：主诉 → 现病史 → 既往史 → 体况检查 → 诊断 → 治疗 → 医嘱
-- 适用数据库：pet_ship

ALTER TABLE `medical_record`
    ADD COLUMN `present_illness` text NULL COMMENT '现病史：症状从出现到本次就诊的发展过程' AFTER `chief_complaint`,
    ADD COLUMN `past_history` text NULL COMMENT '既往病史：过去与本次判断相关的重要医疗信息' AFTER `present_illness`;

-- 病历模板同步扩充（模板可按项预置现病史/既往病史）
ALTER TABLE `record_template`
    ADD COLUMN `present_illness` text NULL COMMENT '现病史模板' AFTER `chief_complaint`,
    ADD COLUMN `past_history` text NULL COMMENT '既往病史模板' AFTER `present_illness`;
