package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新建手术单请求
 */
@Data
public class SurgeryVo {
    /** 病历id（必填） */
    private Integer recordId;
    private String surgeryName;
    private String surgeryType;
    private String anesthesiaType;
    private LocalDateTime scheduledTime;
    private String surgeon;
    private String assistant;
    private BigDecimal fee;
}
