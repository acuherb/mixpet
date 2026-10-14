package com.ship.model.dto;

import com.ship.model.entity.LabOrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 化验单联查DTO：单头 + 宠物/客户/医生名 + 明细
 */
@Data
public class LabOrderDto {
    private Integer orderId;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private String orderNote;
    private BigDecimal fee;
    /** 0待检验 1已出结果 */
    private Boolean status;
    private String resultNote;
    private String resultBy;
    private String reportFile;
    private LocalDateTime createTime;

    private String petName;
    private String clientName;
    private String doctorName;
    /** 临床诊断（联查病历，便于录结果时参考） */
    private String diagnosis;

    /** 化验明细 */
    private List<LabOrderItem> items;
}