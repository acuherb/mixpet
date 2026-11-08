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
    /** 会员折扣快照（1.00=无折扣）；折后应付 real_money = ROUND(total_money * IFNULL(discount,1),2) */
    private BigDecimal discount;
    private BigDecimal discountMoney;
    private BigDecimal realMoney;
    /** 发药核对状态：是否发药/核对人/时间 */
    private Boolean dispenseStatus;
    private String dispenseBy;
    private LocalDateTime dispenseTime;
    /** 退款状态：是否整单退/操作人/原因/时间/累计已退金额 */
    private Boolean refundStatus;
    private String refundBy;
    private String refundReason;
    private LocalDateTime refundTime;
    private BigDecimal refundMoney;
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