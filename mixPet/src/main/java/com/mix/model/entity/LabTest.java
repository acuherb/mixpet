package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检验项目字典实体：血常规/生化/尿检等可勾选的检验项，含分组/单位/参考范围/单价
 */
@Data
public class LabTest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "test_id", type = IdType.AUTO)
    private Integer testId;

    /** 项目名称，如 白细胞计数 */
    private String testName;
    /** 分组：血常规/生化/尿检/镜检 */
    private String testGroup;
    /** 单位，如 10^9/L */
    private String testUnit;
    /** 参考范围，如 6.0-17.0 */
    private String refRange;
    /** 单价(元) */
    private BigDecimal testPrice;
    /** 启停（停用后不可再开单） */
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}