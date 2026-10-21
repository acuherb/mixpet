-- 修复 member_level / record_template 的中文双重编码乱码
-- 根因：这两张表的历史测试数据经某条 charset 不一致的途径写入——中文的 UTF-8 字节被按 Latin1 解读后又按 UTF-8 存了一遍
--      （如「普通」E699AEE9809A 变成 C3A6E284A2C2AE... = æ™®é€š），页面前端原样显示即乱码。
--      应用层 JDBC 无问题（全库仅此两表命中，其他表中文正常）。
-- 修复：latin1→utf8mb4 回转；WHERE 条件保证只转「原值无中文、回转后出现中文」的行，正常中文行不受影响。
-- 已执行并验证：普通/银卡/金卡/钻石、犬瘟热初诊/年度体检/绝育术后 全部还原。

UPDATE `member_level`
SET `level_name` = CONVERT(CAST(CONVERT(`level_name` USING latin1) AS BINARY) USING utf8mb4)
WHERE `level_name` NOT REGEXP '[一-龥]'
  AND CONVERT(CAST(CONVERT(`level_name` USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-龥]';

UPDATE `record_template`
SET `name` = CONVERT(CAST(CONVERT(`name` USING latin1) AS BINARY) USING utf8mb4)
WHERE `name` NOT REGEXP '[一-龥]'
  AND CONVERT(CAST(CONVERT(`name` USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-龥]';

UPDATE `record_template`
SET `category` = CONVERT(CAST(CONVERT(`category` USING latin1) AS BINARY) USING utf8mb4)
WHERE `category` NOT REGEXP '[一-龥]'
  AND CONVERT(CAST(CONVERT(`category` USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-龥]';

UPDATE `record_template`
SET `chief_complaint` = CONVERT(CAST(CONVERT(`chief_complaint` USING latin1) AS BINARY) USING utf8mb4)
WHERE `chief_complaint` NOT REGEXP '[一-龥]'
  AND CONVERT(CAST(CONVERT(`chief_complaint` USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-龥]';

UPDATE `record_template`
SET `diagnosis` = CONVERT(CAST(CONVERT(`diagnosis` USING latin1) AS BINARY) USING utf8mb4)
WHERE `diagnosis` NOT REGEXP '[一-龥]'
  AND CONVERT(CAST(CONVERT(`diagnosis` USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-龥]';

UPDATE `record_template`
SET `treatment` = CONVERT(CAST(CONVERT(`treatment` USING latin1) AS BINARY) USING utf8mb4)
WHERE `treatment` NOT REGEXP '[一-龥]'
  AND CONVERT(CAST(CONVERT(`treatment` USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-龥]';

UPDATE `record_template`
SET `advice` = CONVERT(CAST(CONVERT(`advice` USING latin1) AS BINARY) USING utf8mb4)
WHERE `advice` NOT REGEXP '[一-龥]'
  AND CONVERT(CAST(CONVERT(`advice` USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-龥]';
