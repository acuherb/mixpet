package com.ship.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 病历联查 DTO：就诊史列表用（宠物/客户/医生/科室名 + 费用与支付状态）
 */
@Data
public class RecordDto {
    private Integer recordId;
    private Integer appointmentId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private String chiefComplaint;
    private String diagnosis;
    private String treatment;
    private String advice;
    private LocalDate revisitDate;
    /** 关联的初诊/上次数病历id（复诊链） */
    private Integer originRecordId;
    /** 初诊/上次就诊的诊断（详情接口回填，便于追溯） */
    private String originDiagnosis;
    private BigDecimal feeRegister;
    private BigDecimal feeTreat;
    /** 药费（来自账单） */
    private BigDecimal feeDrug;
    /** 合计（来自账单） */
    private BigDecimal totalMoney;
    /** 支付状态（来自账单，未生成账单为 null） */
    private Boolean payStatus;
    private LocalDateTime createTime;
    /** 就诊状态：0接诊中 1已完结 2已作废 */
    private Integer status;
    /** 完结时间 */
    private LocalDateTime finishTime;
    /** 作废原因 */
    private String cancelReason;
    private String petName;
    private String clientName;
    private String doctorName;
    private String departmentName;
    /** 内部备注：仅病历详情接口返回（员工可见），列表查询不查此列 */
    private String internalNote;
    /** 体格检查：仅病历详情接口返回 */
    private String physicalExam;
}
