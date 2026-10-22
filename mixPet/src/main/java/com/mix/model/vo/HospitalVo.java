package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 入院登记请求
 */
@Data
public class HospitalVo {
    /** 病历id（必填，归属以病历为准） */
    private Integer recordId;
    /** 病房/床位号 */
    private String roomNo;
    /** 日均住院费(元) = 床位+护理+输液（前端汇总传入，兼容旧字段） */
    private BigDecimal dailyFee;
    /** 床位费/天 */
    private BigDecimal bedFee;
    /** 护理费/天 */
    private BigDecimal nursingFee;
    /** 输液费/天 */
    private BigDecimal infusionFee;
    /** 备注 */
    private String note;
}