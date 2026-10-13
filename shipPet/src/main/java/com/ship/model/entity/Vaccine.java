package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 疫苗字典
 */
@Data
public class Vaccine implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "vaccine_id", type = IdType.AUTO)
    private Integer vaccineId;

    private String vaccineName;
    private String brand;
    /** 预防疾病 */
    private String prevent;
    /** 建议接种间隔(天) */
    private Integer intervalDays;
    /** 参考单价(元) */
    private BigDecimal price;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}