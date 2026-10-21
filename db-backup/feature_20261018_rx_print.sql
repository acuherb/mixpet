-- 兽用处方笺打印补充表 + 留痕（农业农村部第734号《兽医处方格式及应用规范》）
-- 处方正文仍以 prescription/medical_record 为准，本表只存「打印时才需要的补充字段 + 编号 + 留痕」
-- 适用数据库：pet_mix

CREATE TABLE IF NOT EXISTS `rx_print_supplement` (
  `id`                BIGINT       NOT NULL AUTO_INCREMENT,
  `prescription_id`   BIGINT       NOT NULL COMMENT '处方行ID(多处方时定位到具体药品行;整单打印取该病历任一处方行，由服务层固定规则)',
  `record_id`         BIGINT       NOT NULL COMMENT '病历ID',
  `print_no`          VARCHAR(32)  NOT NULL COMMENT '处方笺编号 RX-YYYYMMDD-XXXX(按天自增，作废占号不回收)',
  `hair_color`        VARCHAR(16)  DEFAULT NULL COMMENT '毛色(优先pet.pet_color，取不到时补录，不回写pet)',
  `species`           VARCHAR(16)  DEFAULT NULL COMMENT '动物种类(犬/猫/兔…，pet_variety是品种非种类，缺失时补录)',
  `withdrawal_period` VARCHAR(32)  DEFAULT NULL COMMENT '休药期(宠物处方固定"—"，食品动物从药品取，取不到补录)',
  `dispenser`         VARCHAR(50)  DEFAULT NULL COMMENT '发药人(优先bill.dispense_by姓名，未发药时补录或留空手写)',
  `copies`            TINYINT      NOT NULL DEFAULT 1 COMMENT '联次份数 1=仅第三联(畜主留存) 3=三联全打',
  `status`            VARCHAR(8)   NOT NULL DEFAULT 'VALID' COMMENT 'VALID有效 / VOID已作废',
  `printed_by`        BIGINT       DEFAULT NULL COMMENT '打印触发人ID',
  `printed_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近打印时间(留痕，不印处方笺正文)',
  `void_by`           BIGINT       DEFAULT NULL COMMENT '作废操作人',
  `void_at`           DATETIME     DEFAULT NULL COMMENT '作废时间',
  `void_reason`       VARCHAR(255) DEFAULT NULL COMMENT '作废原因',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_no` (`print_no`),
  UNIQUE KEY `uk_prescription` (`prescription_id`),
  KEY `idx_record` (`record_id`),
  KEY `idx_printed_at` (`printed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兽用处方笺打印补充+留痕(正文仍以处方表为准)';
