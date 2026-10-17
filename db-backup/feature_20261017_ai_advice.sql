-- AI 辅助诊疗留痕表（P2 Phase1：接口抽象 + Mock Provider）
-- 留痕内容：谁触发、何时、入参摘要 hash、provider、生成内容 JSON，满足可追溯
-- 适用数据库：pet_ship

CREATE TABLE IF NOT EXISTS `ai_advice` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `record_id`    BIGINT       NULL COMMENT '关联病历',
  `provider`     VARCHAR(32)  NOT NULL DEFAULT 'mock' COMMENT '模型提供方 mock/deepseek',
  `request_hash` VARCHAR(64)  NULL COMMENT '入参摘要hash,用于防重复',
  `advice_json`  TEXT         NULL COMMENT '结构化建议JSON',
  `doctor`       BIGINT       NULL COMMENT '触发医生',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI辅助诊疗留痕';
