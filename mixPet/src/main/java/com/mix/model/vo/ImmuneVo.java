package com.mix.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接种登记请求
 */
@Data
public class ImmuneVo {
    /** 病历id（可空） */
    private Integer recordId;
    /** 宠物id（必填或由病历推导） */
    private Integer petId;
    /** 疫苗药品id（必填，须为 drug_type=疫苗 的药品） */
    private Integer drugId;
    /** 本次接种时间（默认现在） */
    private LocalDateTime vaccinateTime;
    /** 备注 */
    private String note;
}