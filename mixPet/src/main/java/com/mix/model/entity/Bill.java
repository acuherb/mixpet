package com.mix.model.entity;

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
    /** 会员折扣率快照（建单时按客户等级，1=不打折） */
    private BigDecimal discount;
    /** 疫苗费（免疫接种计入） */
    private BigDecimal feeImmune;
    /** 优惠金额 = totalMoney × (1 - discount)（DB 生成列：禁止写入，防 updateById 时报 Generated column 错） */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private BigDecimal discountMoney;
    /** 折后应付/实收 = totalMoney - discountMoney（DB 生成列：禁止写入） */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private BigDecimal realMoney;
    /** 0未付 1已付 */
    private Boolean payStatus;
    /** 0余额 1现金 */
    private Boolean payType;
    /** 收款操作人姓名（收银不限定角色，忙时医生可代收，记录谁收的便于对账） */
    private String payBy;

    /** 微信商户单号（预下单落库，查单/退款用） */
    private String wxOutTradeNo;

    /** 微信支付交易号（回调成功落库，对账用） */
    private String wxTransactionId;
    /** 发药状态：0未发药 1已发药（护士核对后置1） */
    private Boolean dispenseStatus;
    /** 发药核对人 */
    private String dispenseBy;
    /** 发药核对时间 */
    private LocalDateTime dispenseTime;
    /** 退款状态：0未退 1已退（冲正） */
    private Boolean refundStatus;
    /** 退款操作人 */
    private String refundBy;
    /** 退款原因（必填，与操作人分开留痕） */
    private String refundReason;
    /** 退款时间 */
    private LocalDateTime refundTime;
    /** 收款时间：独立于 update_time，退款/追加处方不会刷新，日结按此列统计 */
    private LocalDateTime payTime;
    /** 累计已退金额（实付口径）：账单原始金额不被冲减，报表按 real_money - refund_money 计算 */
    private BigDecimal refundMoney;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}