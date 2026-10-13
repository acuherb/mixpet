package com.ship.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 影像申请单联查 DTO
 */
@Data
public class ImageOrderDto {
    private Integer orderId;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private Integer itemId;
    private String itemName;
    private String groupName;
    private BigDecimal fee;
    private String examPart;
    private String result;
    private LocalDateTime reportTime;
    private Boolean status;
    private String note;
    private LocalDateTime createTime;

    private String petName;
    private String clientName;
    private String doctorName;
    private String diagnosis;
}