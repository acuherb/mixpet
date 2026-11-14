package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 体检表可编辑模板。schemaJson 存「分区 → 项目」树：
 * {title, sections:[{key,title,items:[{key,label,type,side?,options?,unit?,normal?}]}]}
 * 管理员改模板只改这一列，不动表结构。
 */
@Data
@TableName("checkup_template")
public class CheckupTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "template_id", type = IdType.AUTO)
    private Integer templateId;
    /** 模板键（沿用老 mrt 短名，便于追溯） */
    private String tplKey;
    private String name;
    private String category;
    private String description;
    private String schemaJson;
    private Boolean active;
    private Boolean isBuiltin;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long isDel;
}
