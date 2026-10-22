package com.mix.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 住院记录联查 DTO
 */
@Data
public class HospitalDto {
    private Integer hospitalId;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private String roomNo;
    private LocalDateTime inTime;
    private LocalDateTime outTime;
    private BigDecimal dailyFee;
    /** 床位费/天 */
    private BigDecimal bedFee;
    /** 护理费/天 */
    private BigDecimal nursingFee;
    /** 输液费/天 */
    private BigDecimal infusionFee;
    /** 已收押金 */
    private BigDecimal deposit;
    private Integer days;
    private Boolean status;
    private String note;
    private LocalDateTime createTime;

    private String petName;
    private String clientName;
    private String doctorName;
    /** 病历诊断，便于查看入院原因 */
    private String diagnosis;
}