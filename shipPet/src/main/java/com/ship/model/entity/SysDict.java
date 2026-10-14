package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用字典/系统设置项：集中管理各模块的枚举（化验分组、影像分组、药品分类、药品单位、手术类型、麻醉方式等）
 */
@Data
public class SysDict implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "dict_id", type = IdType.AUTO)
    private Integer dictId;

    /** 字典类型：lab_group/img_group/drug_type/drug_unit/surgery_type/anesthesia_type */
    private String dictType;
    /** 字典值（展示与存储都用它） */
    private String dictKey;
    /** 排序（小的在前） */
    private Integer sort;
    /** 启停 */
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
