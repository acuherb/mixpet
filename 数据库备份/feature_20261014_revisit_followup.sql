-- 2026-10-14 夜·收官：复诊关联 + 随访记录

-- 1. 复诊链：挂号单与病历关联初诊/上次数病历
ALTER TABLE appointment ADD COLUMN origin_record_id INT NULL COMMENT '关联的初诊病历id(复诊挂号时填)';
ALTER TABLE medical_record ADD COLUMN origin_record_id INT NULL COMMENT '关联的初诊/上次数病历id(复诊链)';

-- 2. 随访记录
CREATE TABLE IF NOT EXISTS follow_up (
    follow_id    INT AUTO_INCREMENT PRIMARY KEY,
    record_id    INT NOT NULL COMMENT '病历id',
    pet_id       INT NULL,
    client_id    INT NULL,
    follow_type  VARCHAR(32) NULL COMMENT '随访方式 电话/微信/到店/上门',
    content      VARCHAR(500) NULL COMMENT '随访内容/计划',
    result       VARCHAR(500) NULL COMMENT '随访结果/客户反馈',
    plan_time    DATETIME NULL COMMENT '计划随访时间',
    follow_time  DATETIME NULL COMMENT '实际随访时间',
    status       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0待随访 1已随访',
    follow_by    VARCHAR(50) NULL COMMENT '随访人',
    note         VARCHAR(255) NULL,
    create_time  DATETIME NULL,
    KEY idx_record (record_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='随访记录';
