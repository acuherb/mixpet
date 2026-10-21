package com.mix.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 处方笺档案列表项（管理端调档/留档查询）
 */
@Data
public class RxArchiveVo {
    private String printNo;
    /** 开具日期 = prescription.create_time */
    private LocalDateTime prescriptionTime;
    private String clientName;
    private String petName;
    private String doctorName;
    private String doctorCert;
    /** 联次份数 1/3 */
    private Integer copies;
    /** 最近打印时间（留痕） */
    private LocalDateTime printedAt;
    /** VALID / VOID */
    private String status;
    private String voidReason;
    private Integer recordId;
}
