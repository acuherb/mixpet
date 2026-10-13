package com.ship.model.vo;

import com.ship.model.entity.Prescription;
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
}
