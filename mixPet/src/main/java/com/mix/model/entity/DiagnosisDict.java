package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 诊断编码字典：院内自定义码 / ICD-11-VET 等。
 * 病历只存 code + codeSystem 快照，字典后续修订不影响历史病历。
 */
@Data
@TableName("diagnosis_dict")
public class DiagnosisDict implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "dict_id", type = IdType.AUTO)
    private Integer dictId;
    private String code;
    private String name;
    /** INTERNAL / ICD11_VET / SNOMED_CT_VET */
    private String codeSystem;
    private String category;
    /** 适用物种：犬/猫；空=通用 */
    private String species;
    private Boolean active;
}
