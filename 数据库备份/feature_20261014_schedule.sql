-- 2026-10-14：医生排班
CREATE TABLE IF NOT EXISTS doctor_schedule (
    schedule_id   INT AUTO_INCREMENT PRIMARY KEY,
    doctor_id     INT NOT NULL COMMENT '医生id',
    doctor_name   VARCHAR(64) NULL COMMENT '冗余医生名',
    department_id INT NULL,
    weekday       INT NOT NULL COMMENT '1周一..7周日',
    period        INT NOT NULL COMMENT '1上午 2下午',
    max_count     INT NOT NULL DEFAULT 10 COMMENT '可约号源',
    active        TINYINT(1) NOT NULL DEFAULT 1,
    create_time   DATETIME NULL,
    update_time   DATETIME NULL,
    is_del        BIGINT NULL,
    KEY idx_doctor (doctor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班';
