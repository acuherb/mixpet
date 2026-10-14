-- 2026-10-14 夜：住院每日监护记录
CREATE TABLE IF NOT EXISTS hospital_monitor (
    monitor_id      INT AUTO_INCREMENT PRIMARY KEY,
    hospital_id     INT NOT NULL COMMENT '住院单id',
    pet_id          INT NULL COMMENT '宠物id(冗余)',
    monitor_time    DATETIME NULL COMMENT '记录时间',
    temperature     DECIMAL(4,1) NULL COMMENT '体温℃',
    heart_rate      INT NULL COMMENT '心率 次/分',
    respiratory_rate INT NULL COMMENT '呼吸 次/分',
    weight          DECIMAL(6,2) NULL COMMENT '体重kg',
    mental_state    VARCHAR(64) NULL COMMENT '精神/状态',
    appetite        VARCHAR(128) NULL COMMENT '食欲/饮食',
    excretion       VARCHAR(128) NULL COMMENT '排便排尿',
    medication      VARCHAR(255) NULL COMMENT '当日用药',
    treatment       VARCHAR(255) NULL COMMENT '处置/治疗',
    note            VARCHAR(255) NULL COMMENT '备注',
    recorder        VARCHAR(50) NULL COMMENT '记录人',
    create_time     DATETIME NULL,
    KEY idx_hospital (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='住院每日监护记录';
