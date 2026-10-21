package com.mix.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 批量入库单行明细（与单条 /drug/inbound 入参一致）
 */
@Data
public class InboundLine {
    private Integer drugId;
    private Integer quantity;
    /** 本批成本价（元/单位，可空；录入则刷新药品当前进货价并记入台账） */
    private BigDecimal costPrice;
    private String batchNo;
    private LocalDate expireDate;
    private String note;
    private Integer supplierId;
}
