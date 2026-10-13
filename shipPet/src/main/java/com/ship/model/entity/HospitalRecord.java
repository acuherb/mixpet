package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 住院记录实体：入院登记 + 出院结算
 */
@Data
public class HospitalRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "hospital_id", type = IdType.AUTO)
    private Integer hospitalId;

    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    /** 主治医生 */
    private Integer doctorId;
    /** 病房/床位号 */
    private String roomNo;
    /** 入院时间 */
    private LocalDateTime inTime;
    /** 出院时间（出院时写） */
    private LocalDateTime outTime;
    /** 日均住院费(元) */
    private BigDecimal dailyFee;
    /** 住院天数（出院时结算） */
    private Integer days;
    /** 0住院中 1已出院 */
    private Boolean status;
    /** 备注 */
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}