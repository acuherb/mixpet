package com.ship.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处方笺打印补充字段（出参：前端展示 + 补录回显）
 */
@Data
public class RxSupplementVo {
    /** 处方笺编号（未打印为 null） */
    private String printNo;
    private String hairColor;
    private String species;
    /** 休药期（宠物处方固定 "—"） */
    private String withdrawalPeriod;
    private String dispenser;
    /** 联次份数 1/3 */
    private Integer copies;
    /** VALID / VOID */
    private String status;
    /** 最近打印时间（仅留痕，不印处方笺正文） */
    private LocalDateTime printedAt;
}
