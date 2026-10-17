package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 辅助诊疗留痕：记录每次触发（谁/何时/入参摘要/provider/生成内容），满足可追溯
 */
@Data
@TableName("ai_advice")
public class AiAdvice implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long recordId;
    private String provider;
    /** 入参摘要 hash（recordId+主诉+诊断），用于防重复生成 */
    private String requestHash;
    /** 结构化建议 JSON */
    private String adviceJson;
    /** 触发医生 */
    private Long doctor;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
