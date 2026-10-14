-- 2026-10-14 下午：公告隐私分流 + 库存台账 + 退款冲正/单据作废
-- 直接执行；字段/表已存在会报错可忽略（或先 DROP 调试）。

-- 1. 公告：类型（0顾客公告 1系统提醒仅员工可见）
ALTER TABLE notice
    ADD COLUMN notice_type TINYINT NOT NULL DEFAULT 0 COMMENT '0顾客公告(主页可见) 1系统提醒(仅员工可见)';

-- 2. 药品：预警阈值 + 有效期
ALTER TABLE drug
    ADD COLUMN warning_stock INT NOT NULL DEFAULT 0 COMMENT '库存预警阈值',
    ADD COLUMN expire_date DATE NULL COMMENT '有效期至';

-- 3. 药品出入库台账（新表）
CREATE TABLE IF NOT EXISTS drug_record (
    record_id    INT AUTO_INCREMENT PRIMARY KEY,
    drug_id      INT NOT NULL,
    drug_name    VARCHAR(64) NULL,
    change_type  TINYINT NOT NULL COMMENT '1入库 2发药出库 3退款回库 4盘亏/报损',
    quantity     INT NOT NULL,
    before_stock INT NULL,
    after_stock  INT NULL,
    operator     VARCHAR(50) NULL,
    note         VARCHAR(255) NULL,
    create_time  DATETIME NULL,
    KEY idx_drug (drug_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品出入库台账';

-- 4. 账单：退款冲正
ALTER TABLE bill
    ADD COLUMN refund_status TINYINT(1) NOT NULL DEFAULT 0 COMMENT '退款状态 0未退 1已退',
    ADD COLUMN refund_by VARCHAR(50) NULL COMMENT '退款操作人',
    ADD COLUMN refund_time DATETIME NULL COMMENT '退款时间';

-- 5. 单据作废标记
ALTER TABLE prescription ADD COLUMN is_cancel TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否作废';
ALTER TABLE lab_order    ADD COLUMN is_cancel TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否作废';
ALTER TABLE image_order  ADD COLUMN is_cancel TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否作废';
