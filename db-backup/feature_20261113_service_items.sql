-- P0-4 商品/服务项落位（2026-11-13）
-- 背景：PetSystem epbmaster 是全品类料号（76列），IS_STOCK=Y 的非药品零售商品需要 drug_type 有承接值。
-- 化验/影像/手术等服务项 mixpet 已有专属字典（lab_test/image_test/surgery），无需新建服务项表。
-- 适用于：已存在运行库（旧库）升级。全新部署直接导入 pet_mix.sql 即可（下次重压基线时并入）。
-- 可重复执行：NOT EXISTS 判重，重复执行不产生重复行。

-- 1) drug_type 增加「用品」：宠物用品/零食/玩具等零售商品（PetSystem IS_STOCK=Y 非药商品默认归此类）
INSERT INTO sys_dict (dict_type, dict_key, sort, active)
SELECT 'drug_type', '用品', 7, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_type = 'drug_type' AND dict_key = '用品');

-- 2) drug_type 增加「其他」：兜底分类（迁移映射无法判类时的落点）
INSERT INTO sys_dict (dict_type, dict_key, sort, active)
SELECT 'drug_type', '其他', 8, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_dict WHERE dict_type = 'drug_type' AND dict_key = '其他');
