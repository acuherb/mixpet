package com.mix.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物病史-最近就诊摘要
 */
@Data
@AllArgsConstructor
public class PetHistoryRecord {
    private Integer recordId;
    private LocalDateTime createTime;
    private String diagnosis;
    private String doctorName;
}
