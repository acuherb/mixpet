-- 2026-10-15：电子病历模板
CREATE TABLE IF NOT EXISTS record_template (
    template_id     INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL COMMENT '模板名',
    category        VARCHAR(32) NULL COMMENT '分类：常见病/体检/术后/复诊',
    chief_complaint VARCHAR(500) NULL,
    physical_exam   VARCHAR(500) NULL,
    diagnosis       VARCHAR(500) NULL,
    treatment       VARCHAR(500) NULL,
    advice          VARCHAR(500) NULL,
    internal_note   VARCHAR(500) NULL,
    active          TINYINT(1) NOT NULL DEFAULT 1,
    create_time     DATETIME NULL,
    update_time     DATETIME NULL,
    is_del          BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子病历模板';

-- 种子：常用模板
INSERT INTO record_template (name, category, chief_complaint, diagnosis, treatment, advice, active) VALUES
('犬瘟热初诊', '常见病', '精神沉郁、食欲废绝、发热、鼻镜干裂', '疑似犬瘟热（需试纸/核酸确诊）', '补液、抗病毒、对症支持、隔离', '告知预后与传染性，建议住院隔离治疗', 1),
('年度体检', '体检', '主人主诉无异常，年度健康检查', '待检查', '血常规+生化+影像', '禁食8小时后采血', 1),
('绝育术后', '术后', '已行绝育手术，术后复查', '绝育术后恢复期', '复查伤口、镇痛、抗生素', '佩戴伊丽莎白圈，7天拆线', 1);
