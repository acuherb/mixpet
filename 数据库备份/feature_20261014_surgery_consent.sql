-- 2026-10-14 夜：手术/麻醉知情同意书
CREATE TABLE IF NOT EXISTS surgery_consent (
    consent_id     INT AUTO_INCREMENT PRIMARY KEY,
    surgery_id     INT NOT NULL COMMENT '手术单id',
    record_id      INT NULL,
    pet_id         INT NULL,
    client_id      INT NULL,
    surgery_name   VARCHAR(128) NULL COMMENT '冗余手术名',
    anesthesia_type VARCHAR(64) NULL COMMENT '冗余麻醉方式',
    risks          VARCHAR(1000) NULL COMMENT '风险与注意事项告知',
    agree          TINYINT(1) NULL COMMENT '畜主是否同意',
    signer         VARCHAR(64) NULL COMMENT '签字人(畜主姓名)',
    signed_time    DATETIME NULL COMMENT '签署时间',
    operator       VARCHAR(50) NULL COMMENT '经办医护',
    note           VARCHAR(255) NULL,
    create_time    DATETIME NULL,
    UNIQUE KEY uk_surgery (surgery_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='手术/麻醉知情同意书';
