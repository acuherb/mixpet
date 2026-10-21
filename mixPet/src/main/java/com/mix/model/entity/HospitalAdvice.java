package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 住院医嘱单：住院期间的每日用药/治疗/护理计划（逐条下达、停嘱留痕）
 */
@Data
public class HospitalAdvice implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "advice_id", type = IdType.AUTO)
    private Integer adviceId;

    private Integer hospitalId;
    private Integer recordId;
    private Integer doctorId;
    /** 下达医生姓名（快照，防医生改名后历史医嘱失真） */
    private String doctorName;
    /** 类型：1药品 2治疗 3护理 */
    private Integer itemType;
    /** 药品id（药品类医嘱，与药品/处方联动） */
    private Integer drugId;
    private String itemName;
    /** 单次剂量 */
    private String dose;
    private String doseUnit;
    /** 频次：QD/BID/TID/QID/持续 */
    private String frequency;
    /** 途径：口服/注射/外用/静脉 */
    private String route;
    /** 计划天数 */
    private Integer days;
    /** 开始日期 YYYY-MM-DD */
    private String startDate;
    private String remark;
    /** 状态：1待执行 2执行中 3已停嘱 4已完成 */
    private Integer status;
    /** 停嘱原因（停嘱时必填，医疗留痕不删除） */
    private String stopReason;
    private LocalDateTime stopTime;
    private String stopBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
