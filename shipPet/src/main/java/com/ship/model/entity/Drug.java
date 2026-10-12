package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品字典实体
 */
@Data
public class Drug implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "drug_id", type = IdType.AUTO)
    private Integer drugId;

    private String drugName;
    /** 规格，如 0.25g×24片 */
    private String drugSpec;
    /** 单位：盒/瓶/支 */
    private String drugUnit;
    /** 分类：西药/中成药/外用/疫苗/驱虫/保健 */
    private String drugType;
    private BigDecimal drugPrice;
    private Integer drugStock;
    private String drugInfo;
    /** 启停（停用后不可再开方） */
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
