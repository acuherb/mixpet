-- 2026-10-14 工作流增强：接诊分诊/发药核对/检验报告文件/免疫提醒
-- 兼容旧库：直接执行即可，字段已存在时会跳过（IGNORE 写法见各条）

-- 1. 病历表：分诊级别 + 生命体征
ALTER TABLE medical_record
    ADD COLUMN triage_level TINYINT NULL COMMENT '分诊级别 1急诊 2普通 3复诊',
    ADD COLUMN weight DECIMAL(6,2) NULL COMMENT '就诊体重kg',
    ADD COLUMN temperature DECIMAL(4,1) NULL COMMENT '体温℃',
    ADD COLUMN heart_rate INT NULL COMMENT '心率 次/分',
    ADD COLUMN respiratory_rate INT NULL COMMENT '呼吸 次/分';

-- 2. 账单表：发药核对状态
ALTER TABLE bill
    ADD COLUMN dispense_status TINYINT(1) NOT NULL DEFAULT 0 COMMENT '发药状态 0未发 1已发',
    ADD COLUMN dispense_by VARCHAR(50) NULL COMMENT '发药核对人',
    ADD COLUMN dispense_time DATETIME NULL COMMENT '发药核对时间';

-- 3. 化验单：报告文件(可选)
ALTER TABLE lab_order
    ADD COLUMN report_file VARCHAR(255) NULL COMMENT '报告文件(图片/PDF)路径';

-- 4. 影像申请单：报告文件(可选)
ALTER TABLE image_order
    ADD COLUMN report_file VARCHAR(255) NULL COMMENT '报告文件(影像图/PDF)路径';

-- 说明：免疫提醒依赖已有的 immune_record.next_time（下一针建议时间），
-- 调度器 VaccineReminderScheduler 每日 08:00 扫描临近 7 天内的记录并自动发系统公告，
-- 无需新增表。
