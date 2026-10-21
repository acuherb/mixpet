package com.mix.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接种档案联查 DTO
 */
@Data
public class ImmuneRecordDto {
    private Integer immuneId;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private Integer drugId;
    private String drugName;
    private Integer doseCnt;
    private LocalDateTime vaccinateTime;
    private LocalDateTime nextTime;
    private String note;
    private LocalDateTime createTime;

    private String petName;
    private String clientName;
    private String doctorName;
}