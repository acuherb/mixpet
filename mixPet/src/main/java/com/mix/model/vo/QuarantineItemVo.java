package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 检疫清单行（开具时快照，落 quarantine_cert.items_json）
 * 字段与前端打印表一一对应：药品名称/规格/数量/实际用量
 */
@Data
public class QuarantineItemVo {
    private String drugName;
    private String drugSpec;
    private String drugUnit;
    private Integer quantity;
    /** 实际用量文本，如 单次0.5片×每日2次×3天 */
    private String dosage;
    private String usageInfo;
    private BigDecimal singleDose;
    private String singleDoseUnit;
    private Integer dailyTimes;
    private Integer durationDays;
}
