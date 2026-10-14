package com.ship.model.entity;

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
    /** 就诊体重(kg) */
    private BigDecimal weight;
    /** 体温(℃) */
    private BigDecimal temperature;
    /** 心率(次/分) */
    private Integer heartRate;
    /** 呼吸(次/分) */
    private Integer respiratoryRate;

    /** 挂号费 */
    private BigDecimal feeRegister;
    /** 诊疗费 */
    private BigDecimal feeTreat;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    private Long isDel;
}
