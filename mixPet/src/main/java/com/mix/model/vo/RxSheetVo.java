package com.mix.model.vo;

import com.mix.model.entity.Prescription;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 兽医处方笺数据（国标《兽医处方格式及应用规范》）
 * 由病历 + 宠物 + 主人 + 医生 + 处方聚合而成
 */
@Data
public class RxSheetVo {
    private Integer recordId;
    /** 处方编号 */
    private String rxNo;
    /** 开方日期 */
    private LocalDateTime createTime;

    /** 畜主姓名 */
    private String clientName;
    /** 宠物名 */
    private String petName;
    /** 品种 */
    private String petVariety;
    /** 性别（true公 false母） */
    private Boolean petSex;
    /** 生日（用于算年龄） */
    private LocalDate petBirthday;
    /** 体重(kg) */
    private BigDecimal petWeight;
    /** 芯片号 */
    private String petChip;

    /** 临床诊断 */
    private String diagnosis;

    /** 开方兽医 */
    private String doctorName;
    /** 执业兽医资格证号 */
    private String doctorCert;
    /** 科室 */
    private String departmentName;

    /** 处方明细（含快照名称/规格/单位/单价/数量/用法） */
    private List<Prescription> items;
    /** 兽药金额合计 */
    private BigDecimal totalAmount;

    /** 开具日期 = prescription.create_time（为空回落 medical_record.create_time） */
    private LocalDateTime prescriptionTime;
    /** 处方笺编号 RX-YYYYMMDD-XXXX（未打印为 null，打印时自动生成） */
    private String printNo;
    /** 联次份数 1=仅第三联 3=三联全打 */
    private Integer copies;
    /** 毛色（pet.pet_color 或补录） */
    private String hairColor;
    /** 动物种类（补录） */
    private String species;
    /** 休药期（宠物处方 "—"） */
    private String withdrawalPeriod;
    /** 发药人（bill.dispense_by 或补录；空则处方笺留手写位） */
    private String dispenser;
    /** 处方笺状态 VALID/VOID（未打印为 null） */
    private String supplementStatus;

    /** 就诊号（medical_record.appointment_id，734号公告处方笺「就诊号」栏） */
    private Integer appointmentId;
}
