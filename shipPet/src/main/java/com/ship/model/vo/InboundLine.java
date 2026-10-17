package com.ship.model.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 批量入库单行明细（与单条 /drug/inbound 入参一致）
 */
@Data
public class InboundLine {
    private Integer drugId;
    private Integer quantity;
    private String batchNo;
    private LocalDate expireDate;
    private String note;
    private Integer supplierId;
}
