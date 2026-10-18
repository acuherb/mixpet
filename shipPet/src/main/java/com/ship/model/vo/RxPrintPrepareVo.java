package com.ship.model.vo;

import lombok.Data;

/**
 * 打印前预检入参：定位处方 + 补录字段 + 联次
 */
@Data
public class RxPrintPrepareVo {
    private Integer recordId;
    /** 多处方时定位具体处方行；为空取该病历第一行 */
    private Integer prescriptionId;
    /** 补录：毛色（病历/宠物已有则只读展示，缺失才传） */
    private String hairColor;
    /** 补录：动物种类 */
    private String species;
    /** 补录：休药期（宠物处方前端固定传 "—"） */
    private String withdrawalPeriod;
    /** 补录：发药人（未发药时） */
    private String dispenser;
    /** 联次 1=仅第三联(畜主留存) 3=三联全打 */
    private Integer copies;
}
