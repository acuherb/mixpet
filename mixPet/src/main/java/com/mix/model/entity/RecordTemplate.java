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
 * 电子病历模板：预置主诉/检查/诊断/治疗/医嘱，接诊时一键套用
 */
@Data
public class RecordTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "template_id", type = IdType.AUTO)
    private Integer templateId;

    private String name;
    /** 分类：体检/常见病/术后/复诊等 */
    private String category;
    private String chiefComplaint;
    /** 现病史模板 */
    private String presentIllness;
    /** 既往病史模板 */
    private String pastHistory;
    private String physicalExam;
    private String diagnosis;
    private String treatment;
    private String advice;
    private String internalNote;
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
