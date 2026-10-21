package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    /** 进货价(成本价)，用于药品毛利核算 */
    private BigDecimal costPrice;
    private Integer drugStock;
    /** 库存预警阈值：stock <= 该值即低库存预警 */
    private Integer warningStock;
    /** 有效期至（最近批次） */
    private LocalDate expireDate;
    private String drugInfo;
    /** 品牌/厂家（疫苗用） */
    private String brand;
    /** 预防疾病（疫苗用） */
    private String prevent;
    /** 建议接种间隔天数（疫苗用，用于推算下一针） */
    private Integer intervalDays;
    /** 每kg体重单次用量（单位同药剂单位，如 0.1 表示 0.1mL/kg），空=不自动算量 */
    private BigDecimal dosePerKg;
    /** 固定单次用量（与体重无关，优先于 dosePerKg），空=不自动算量 */
    private BigDecimal doseFixed;
    /** 启停（停用后不可再开方） */
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
