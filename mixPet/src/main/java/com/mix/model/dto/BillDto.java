package com.mix.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单联查 DTO：收费列表用
 */
@Data
public class BillDto {
    private Integer billId;
    private Integer clientId;
    private Integer petId;
    private Integer recordId;
    private BigDecimal feeRegister;
    private BigDecimal feeTreat;
    private BigDecimal feeDrug;
    private BigDecimal feeLab;
    private BigDecimal feeHospital;
    private BigDecimal feeImage;
    private BigDecimal feePackage;
    private BigDecimal feeSurgery;
    private BigDecimal feeImmune;
    private BigDecimal totalMoney;
    private Boolean payStatus;
    private Boolean payType;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private String clientName;
    private String clientTel;
    private String petName;
    private String doctorName;
    /** 收款操作人（收银者；历史账单为 null） */
    private String payBy;
    /** 客户当前余额（收款时参考） */
    private BigDecimal clientBalance;
}