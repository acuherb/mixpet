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
 * 麻醉记录（一手术一条）
 */
@Data
public class AnesthesiaRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "anesthesia_id", type = IdType.AUTO)
    private Integer anesthesiaId;

    private Integer surgeryId;
    /** 麻醉方式 */
    private String anesthesiaType;
    /** 麻醉师 */
    private String anesthetist;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 麻醉用药 */
    private String drugUsed;
    /** 生命体征监护记录 */
    private String vitals;
    /** 备注/异常 */
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
