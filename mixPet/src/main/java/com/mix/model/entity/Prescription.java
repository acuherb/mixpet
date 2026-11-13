package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方明细实体：药品名称/规格/单价在开方时冗余快照，不受药品库后续改价影响
 */
@Data
public class Prescription implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "prescription_id", type = IdType.AUTO)
    private Integer prescriptionId;

    private Integer recordId;
    private Integer drugId;
    /** 冗余：开方时药品名 */
    private String drugName;
    private String drugSpec;
    private String drugUnit;
    /** 冗余：开方时单价 */
    private BigDecimal drugPrice;
    private Integer quantity;
    /** 频次：每日几次（结构化，数量=单次剂量×频次×天数） */
    private Integer dailyTimes;
    /** 单次剂量(单位同药剂单位) */
    private BigDecimal singleDose;
    /** 单次剂量单位(片/mL/袋等) */
    private String singleDoseUnit;
    /** 用药天数（734号公告处方笺「天数」列；数量=单次剂量×频次×天数） */
    private Integer durationDays;
    /** 用法，如 每日2次，每次1片 */
    private String usageInfo;
    /** 是否作废 0否 1是（作废后不参与发药/计费） */
    private Boolean isCancel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
