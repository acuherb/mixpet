package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 住院每日监护记录
 */
@Data
public class HospitalMonitor implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "monitor_id", type = IdType.AUTO)
    private Integer monitorId;

    private Integer hospitalId;
    /** 冗余宠物id，便于查询 */
    private Integer petId;
    /** 记录时间 */
    private LocalDateTime monitorTime;
    /** 体温(℃) */
    private BigDecimal temperature;
    /** 心率(次/分) */
    private Integer heartRate;
    /** 呼吸(次/分) */
    private Integer respiratoryRate;
    /** 体重(kg) */
    private BigDecimal weight;
    /** 精神/状态 */
    private String mentalState;
    /** 食欲/饮食 */
    private String appetite;
    /** 排便排尿 */
    private String excretion;
    /** 当日用药 */
    private String medication;
    /** 处置/治疗 */
    private String treatment;
    /** 备注 */
    private String note;
    /** 记录人 */
    private String recorder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
