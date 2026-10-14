package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费账单实体：接诊保存病历时自动生成，收款后标记已付并发药
 */
@Data
public class Bill implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "bill_id", type = IdType.AUTO)
    private Integer billId;

    private Integer clientId;
    private Integer petId;
    private Integer recordId;
    /** 挂号费 */
    private BigDecimal feeRegister;
    /** 诊疗费 */
    private BigDecimal feeTreat;
    /** 药费 */
    private BigDecimal feeDrug;
    /** 化验费 */
    private BigDecimal feeLab;
    /** 住院费 */
    private BigDecimal feeHospital;
    /** 影像费 */
    private BigDecimal feeImage;
    /** 套餐费 */
    private BigDecimal feePackage;
    /** 手术费 */
    private BigDecimal feeSurgery;
    private BigDecimal totalMoney;
    /** 0未付 1已付 */
    private Boolean payStatus;
    /** 0余额 1现金 */
    private Boolean payType;
    /** 发药状态：0未发药 1已发药（护士核对后置1） */
    private Boolean dispenseStatus;
    /** 发药核对人 */
    private String dispenseBy;
    /** 发药核对时间 */
    private LocalDateTime dispenseTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}