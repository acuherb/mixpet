package com.mix.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 随访记录联查 DTO（含宠物/客户/诊断）
 */
@Data
public class FollowUpDto {
    private Integer followId;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private String petName;
    private String clientName;
    private String diagnosis;
    private String followType;
    private String content;
    private String result;
    private LocalDateTime planTime;
    private LocalDateTime followTime;
    private Boolean status;
    private String followBy;
    private String note;
    private LocalDateTime createTime;
}
