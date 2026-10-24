-- 2026-10-15：供应商管理 + 采购入库(供应商/批号) + 近效期预警
CREATE TABLE IF NOT EXISTS supplier (
    supplier_id   INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(128) NOT NULL COMMENT '供应商名称',
    contact       VARCHAR(64) NULL COMMENT '联系人',
    phone         VARCHAR(32) NULL,
    address       VARCHAR(255) NULL,
    note          VARCHAR(255) NULL,
    active        TINYINT(1) NOT NULL DEFAULT 1,
    create_time   DATETIME NULL,
    update_time   DATETIME NULL,
    is_del        BIGINT NULL,
    KEY idx_name (supplier_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商';

-- 台账补供应商/批号（追溯采购来源）
ALTER TABLE drug_record ADD COLUMN supplier_id INT NULL COMMENT '供应商id(入库)';
ALTER TABLE drug_record ADD COLUMN supplier_name VARCHAR(128) NULL COMMENT '冗余供应商名';
ALTER TABLE drug_record ADD COLUMN batch_no VARCHAR(64) NULL COMMENT '批号';
