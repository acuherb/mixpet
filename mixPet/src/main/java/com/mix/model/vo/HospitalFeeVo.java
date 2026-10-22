package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 住院费用预览/结算明细（出院结算弹窗用）
 */
@Data
public class HospitalFeeVo {
    private Integer hospitalId;
    /** 结算天数（默认跨夜+1，可手改） */
    private Integer days;
    /** 床位费/天 */
    private BigDecimal bedFee;
    /** 护理费/天 */
    private BigDecimal nursingFee;
    /** 输液费/天 */
    private BigDecimal infusionFee;
    /** 日均合计 */
    private BigDecimal perDay;
    /** 住院天数小计 = perDay × days */
    private BigDecimal stayFee;
    /** 医嘱临时费用合计（已停嘱未执行不计） */
    private BigDecimal adviceFee;
    /** 费用总计 = stayFee + adviceFee */
    private BigDecimal totalFee;
    /** 已收押金 */
    private BigDecimal deposit;
    /** 应退 = deposit - totalFee（负数=需补收） */
    private BigDecimal refund;
}
