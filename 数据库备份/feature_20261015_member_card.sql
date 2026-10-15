-- 2026-10-15：会员卡/次卡/积分闭环
CREATE TABLE IF NOT EXISTS member_level (
    level_id       INT AUTO_INCREMENT PRIMARY KEY,
    level_key      INT NOT NULL COMMENT '等级键 0普通1银2金3钻石(对应client.client_level)',
    level_name     VARCHAR(32) NOT NULL,
    discount       DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '折扣率 1=不打折',
    recharge_bonus DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '充值返赠比例',
    points_rate    DECIMAL(5,2) NOT NULL DEFAULT 1.00 COMMENT '消费积分率 1元=N分',
    sort           INT NOT NULL DEFAULT 0,
    active         TINYINT(1) NOT NULL DEFAULT 1,
    create_time    DATETIME NULL,
    update_time    DATETIME NULL,
    is_del         BIGINT NULL,
    UNIQUE KEY uk_key (level_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级/会员卡模板';

CREATE TABLE IF NOT EXISTS count_card_template (
    template_id   INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(128) NOT NULL,
    service_type  VARCHAR(64) NULL,
    times         INT NOT NULL COMMENT '次数',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0,
    validity_days INT NULL COMMENT '有效天数',
    active        TINYINT(1) NOT NULL DEFAULT 1,
    create_time   DATETIME NULL,
    update_time   DATETIME NULL,
    is_del        BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='次卡模板';

CREATE TABLE IF NOT EXISTS client_count_card (
    card_id     INT AUTO_INCREMENT PRIMARY KEY,
    client_id   INT NOT NULL,
    template_id INT NULL,
    name        VARCHAR(128) NULL,
    remain_times INT NOT NULL,
    total_times  INT NOT NULL,
    expire_date DATE NULL,
    status      TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1有效 0用完/作废',
    create_time DATETIME NULL,
    KEY idx_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户次卡';

-- 种子：会员等级
INSERT INTO member_level (level_key, level_name, discount, recharge_bonus, points_rate, sort, active) VALUES
(0,'普通',1.00,0.00,1,0,1),
(1,'银卡',0.95,0.05,1,1,1),
(2,'金卡',0.90,0.10,2,2,1),
(3,'钻石',0.85,0.15,3,3,1);
