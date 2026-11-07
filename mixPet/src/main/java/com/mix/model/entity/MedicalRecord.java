package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 电子病历实体：一张挂号单对应一份病历
 */
@Data
public class MedicalRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "record_id", type = IdType.AUTO)
    private Integer recordId;

    /** 关联的挂号单 */
    private Integer appointmentId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private Integer departmentId;

    /** 主诉 */
    private String chiefComplaint;
    /** 现病史：症状从出现到本次就诊的发展过程 */
    private String presentIllness;
    /** 既往病史：过去与本次判断相关的重要医疗信息 */
    private String pastHistory;
    /** 体格检查 */
    private String physicalExam;
    /** 诊断 */
    private String diagnosis;
    /** 治疗方案 */
    private String treatment;
    /** 医嘱 */
    private String advice;
    /** 内部备注（顾客端不可见） */
    private String internalNote;
    /** 复诊日期 */
    private LocalDate revisitDate;

    /** 关联的初诊/上次数病历id（复诊链） */
    private Integer originRecordId;

    /** 分诊级别：1急诊 2普通 3复诊（顾客端walk-in预约） */
    private Integer triageLevel;

    /** 就诊状态：0接诊中 1已完结 2已作废 */
    private Integer status;
    /** 完结时间 */
    private LocalDateTime finishTime;
    /** 作废原因 */
    private String cancelReason;
    /** 就诊体重(kg) */
    private BigDecimal weight;
    /** 体温(℃) */
    private BigDecimal temperature;
    /** 心率(次/分) */
    private Integer heartRate;
    /** 呼吸(次/分) */
    private Integer respiratoryRate;
    /** 血压(收缩/舒张,如 120/80) */
    private String bloodPressure;

    /** 挂号费 */
    private BigDecimal feeRegister;
    /** 诊疗费 */
    private BigDecimal feeTreat;

    /** 病历版本号：建档为 1，每次内容修改 +1（配合前端提交做乐观锁） */
    private Integer version;
    /** 完结签名：接诊医师姓名与签名时间（打印页取真实值，不再留空白） */
    private String signedBy;
    private LocalDateTime signedAt;
    /** 审核状态：0待审核 1已审核 2退回 */
    private Integer reviewStatus;
    private String reviewBy;
    private LocalDateTime reviewAt;
    /** 审核退回理由（修改病历后自动清空） */
    private String reviewNote;

    /** 鉴别诊断：需排除的疾病与理由（SOAP 的 A） */
    private String differentialDiagnosis;
    /** 诊断依据：症状 / 体征 / 检验 / 影像支持点 */
    private String diagnosisBasis;
    /** 诊断编码：院内码或 ICD-11-VET 等；编码体系单独存，便于后续对接医保/上报 */
    private String diagnosisCode;
    private String diagnosisCodeSystem;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    private Long isDel;
}
