-- 公告排序权重：notice 增加 sort(号大越靠前)，让新公告/置顶公告能排在主页前面
-- 适用数据库：pet_ship
-- 说明：主页/后台均按 sort 降序、创建时间降序排列；历史数据用 notice_id 回填，保证新公告靠前

ALTER TABLE `notice`
    ADD COLUMN `sort` int NOT NULL DEFAULT 0 COMMENT '排序权重，号大越靠前' AFTER `notice_type`;

-- 历史公告按 id 回填 sort（id 越大越新，排序后新公告靠前）
UPDATE `notice` SET `sort` = `notice_id` WHERE `sort` = 0;
