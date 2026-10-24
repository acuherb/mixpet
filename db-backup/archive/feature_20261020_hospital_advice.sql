-- 住院医嘱单（清单5：住院每日用药/治疗计划，与处方联动）
-- 设计：单表逐条下达/停嘱（住院医嘱实际就是逐条管理，不建头行结构）
-- 状态：1待执行 2执行中 3已停嘱 4已完成；停嘱留原因不删除（医疗留痕）

CREATE TABLE IF NOT EXISTS `hospital_advice` (
    `advice_id`    int(11)      NOT NULL AUTO_INCREMENT COMMENT '医嘱id',
    `hospital_id`  int(11)      NOT NULL COMMENT '住院记录id',
    `record_id`    int(11)      NOT NULL COMMENT '关联病历id（出院结算/病历视图用）',
    `doctor_id`    int(11)      DEFAULT NULL COMMENT '下达医生id',
    `doctor_name`  varchar(64)  DEFAULT NULL COMMENT '下达医生姓名（快照）',
    `item_type`    tinyint(1)   NOT NULL DEFAULT 1 COMMENT '类型：1药品 2治疗 3护理',
    `drug_id`      int(11)      DEFAULT NULL COMMENT '药品id（药品类医嘱，联动药品表）',
    `item_name`    varchar(128) NOT NULL COMMENT '项目名（药品名/治疗/护理项）',
    `dose`         varchar(32)  DEFAULT NULL COMMENT '单次剂量',
    `dose_unit`    varchar(16)  DEFAULT NULL COMMENT '剂量单位',
    `frequency`    varchar(16)  DEFAULT NULL COMMENT '频次：QD/BID/TID/QID/持续',
    `route`        varchar(32)  DEFAULT NULL COMMENT '途径：口服/注射/外用/静脉',
    `days`         int(11)      DEFAULT NULL COMMENT '计划天数',
    `start_date`   varchar(10)  DEFAULT NULL COMMENT '开始日期 YYYY-MM-DD',
    `remark`       varchar(255) DEFAULT NULL COMMENT '备注',
    `status`       tinyint(1)   NOT NULL DEFAULT 1 COMMENT '1待执行 2执行中 3已停嘱 4已完成',
    `stop_reason`  varchar(255) DEFAULT NULL COMMENT '停嘱原因（停嘱时必填）',
    `stop_time`    datetime     DEFAULT NULL COMMENT '停嘱时间',
    `stop_by`      varchar(50)  DEFAULT NULL COMMENT '停嘱操作人',
    `create_time`  datetime     DEFAULT NULL COMMENT '下达时间',
    `update_time`  datetime     DEFAULT NULL COMMENT '更新时间',
    `is_del`       bigint(20)   DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`advice_id`),
    KEY `idx_hospital` (`hospital_id`),
    KEY `idx_record` (`record_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '住院医嘱单';
