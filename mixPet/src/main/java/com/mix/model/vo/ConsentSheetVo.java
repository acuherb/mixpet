package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 手术/麻醉知情同意书（可打印）：由手术单 + 宠物 + 主人 + 医生 + 病历 + 同意书聚合
 */
@Data
public class ConsentSheetVo {
    private Integer surgeryId;
    /** 同意书编号 */
    private String consentNo;
    private LocalDateTime createTime;

    private String clientName;
    private String petName;
    private String petVariety;
    private Boolean petSex;
    private LocalDate petBirthday;
    private BigDecimal petWeight;
    private String petChip;

    private String diagnosis;

    private String surgeryName;
    private String surgeryType;
    private String anesthesiaType;
    private LocalDateTime scheduledTime;
    private String doctorName;

    /** 风险告知 */
    private String risks;
    /** 是否同意 */
    private Boolean agree;
    /** 签字人 */
    private String signer;
    private LocalDateTime signedTime;
    /** 经办医护 */
    private String operator;
}
