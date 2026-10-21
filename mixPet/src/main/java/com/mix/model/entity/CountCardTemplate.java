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
 * 次卡模板：如 洗澡次卡10次 / 疫苗次卡3次
 */
@Data
public class CountCardTemplate implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "template_id", type = IdType.AUTO)
    private Integer templateId;

    private String name;
    /** 适用服务/项目 */
    private String serviceType;
    /** 次数 */
    private Integer times;
    private BigDecimal price;
    /** 有效天数（自购买起） */
    private Integer validityDays;
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
