package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 寄养单。fosterMoney 只表示登记时预收的押金，接回结算不再覆盖它；
 * 结算结果写 settleMoney / dayCount / depositUsed / refundMoney / chargeMoney / billId，
 * 便于顾客查询与账单对账。
 */
@Data
public class Foster implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "foster_id", type = IdType.AUTO)
    private Integer fosterId;
    /** 订单编号 */
    private String fosterCode;
    private Integer petId;
    private Integer clientId;
    /** 预计接回日期 */
    private LocalDate fosterTerm;
    /** 入托日期 */
    private LocalDate inDate;
    /** 实际接回日期 */
    private LocalDate backDate;
    /** 寄养押金（登记时预收，不再被结算覆盖） */
    private Integer fosterMoney;
    /** 笼位费/天 */
    private BigDecimal dailyFee;
    /** 清洁费/天 */
    private BigDecimal cleanFee;
    /** 餐费/天 */
    private BigDecimal mealFee;
    /** 0待入托 1寄养中 2已接回 */
    private Integer status;
    private String fosterInfo;

    /** 实际寄养天数（结算时落库） */
    private Integer dayCount;
    /** 最终结算定价（含人工改价） */
    private BigDecimal settleMoney;
    /** 押金冲抵金额 */
    private BigDecimal depositUsed;
    /** 应退金额 */
    private BigDecimal refundMoney;
    /** 实际计入账单的净额 */
    private BigDecimal chargeMoney;
    /** 人工改价说明（留痕） */
    private String adjustNote;
    /** 接回结算生成的账单id */
    private Integer billId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    /** 逻辑删除（项目全局 isDel） */
    private Long isDel;
}
