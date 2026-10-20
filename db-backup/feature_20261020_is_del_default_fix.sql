-- 修复 is_del 列默认值缺失导致「新增成功但列表查不到」
-- 根因：MP 全局配置 logic-delete-field=isDel，插入时 isDel 为 null 不拼该列、值走列默认值；
-- bill/drug 等表 DEFAULT 0 正常，而下列 4 张表 DEFAULT NULL → 插入 NULL 被 is_del=0 过滤掉。
-- 修复：列默认值改 0（根治所有插入路径）+ 存量 NULL 行找回（不影响真实逻辑删除的行——其 is_del 为删除时间戳非 NULL）

ALTER TABLE `supplier`          MODIFY COLUMN `is_del` bigint(20) DEFAULT 0;
ALTER TABLE `member_level`      MODIFY COLUMN `is_del` bigint(20) DEFAULT 0;
ALTER TABLE `record_template`   MODIFY COLUMN `is_del` bigint(20) DEFAULT 0;
ALTER TABLE `count_card_template` MODIFY COLUMN `is_del` bigint(20) DEFAULT 0;

UPDATE `supplier`            SET `is_del` = 0 WHERE `is_del` IS NULL;
UPDATE `member_level`        SET `is_del` = 0 WHERE `is_del` IS NULL;
UPDATE `record_template`     SET `is_del` = 0 WHERE `is_del` IS NULL;
UPDATE `count_card_template` SET `is_del` = 0 WHERE `is_del` IS NULL;
