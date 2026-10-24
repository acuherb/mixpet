-- 2026-10-14：通用字典/系统设置（集中管理各模块枚举）
CREATE TABLE IF NOT EXISTS sys_dict (
    dict_id     INT AUTO_INCREMENT PRIMARY KEY,
    dict_type   VARCHAR(32) NOT NULL COMMENT '字典类型 lab_group/img_group/drug_type/drug_unit/surgery_type/anesthesia_type',
    dict_key    VARCHAR(64) NOT NULL COMMENT '字典值',
    sort        INT NOT NULL DEFAULT 0 COMMENT '排序',
    active      TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启停',
    create_time DATETIME NULL,
    KEY idx_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用字典/系统设置';

-- 种子数据
INSERT INTO sys_dict (dict_type, dict_key, sort, active) VALUES
('drug_unit','盒',1,1),('drug_unit','瓶',2,1),('drug_unit','支',3,1),('drug_unit','袋',4,1),('drug_unit','毫升',5,1),('drug_unit','片',6,1),
('drug_type','西药',1,1),('drug_type','中成药',2,1),('drug_type','外用',3,1),('drug_type','疫苗',4,1),('drug_type','驱虫',5,1),('drug_type','保健',6,1),
('lab_group','血常规',1,1),('lab_group','生化',2,1),('lab_group','尿检',3,1),('lab_group','镜检',4,1),
('img_group','X光',1,1),('img_group','B超',2,1),('img_group','CT',3,1),
('surgery_type','绝育',1,1),('surgery_type','骨科',2,1),('surgery_type','肿瘤',3,1),('surgery_type','外伤',4,1),('surgery_type','其他',5,1),
('anesthesia_type','全身麻醉',1,1),('anesthesia_type','局部麻醉',2,1),('anesthesia_type','镇静',3,1);
