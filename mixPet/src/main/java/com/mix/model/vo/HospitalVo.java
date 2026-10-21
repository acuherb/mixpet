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
    /** 日均住院费(元) */
    private BigDecimal dailyFee;
    /** 备注 */
    private String note;
}