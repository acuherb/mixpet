-- 服务套餐结构化：套餐由「检查项目」组合而成（不再只是名字+定价）
-- package_item：套餐包含的检验/影像项目（快照名称与价格，字典调价不影响历史套餐展示）
-- 注意：is_del 必须 DEFAULT 0（MP 插入不拼该列，NULL 会被逻辑删除过滤——2026-10-20 supplier 坑的教训）

CREATE TABLE IF NOT EXISTS `package_item` (
    `id`          int(11)      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `package_id`  int(11)      NOT NULL COMMENT '套餐id',
    `item_type`   varchar(8)   NOT NULL COMMENT 'lab 检验 / image 影像',
    `item_id`     int(11)      NOT NULL COMMENT '项目id(检验项/影像项)',
    `item_name`   varchar(128) NOT NULL COMMENT '项目名快照',
    `item_price`  decimal(10, 2) NULL COMMENT '项目单价快照(组合时汇总原价参考)',
    `create_time` datetime     NULL COMMENT '创建时间',
    `is_del`      bigint(20)   DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_package` (`package_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '套餐包含项目（检验/影像）';
