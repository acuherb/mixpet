package com.ship.model.entity;

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
    /** 用法，如 每日2次，每次1片 */
    private String usageInfo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
