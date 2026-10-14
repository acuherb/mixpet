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
 * 医生排班：某医生每周几的上午/下午是否出诊及可约号源数
 */
@Data
public class DoctorSchedule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "schedule_id", type = IdType.AUTO)
    private Integer scheduleId;

    private Integer doctorId;
    /** 冗余医生名，便于总览展示 */
    private String doctorName;
    private Integer departmentId;
    /** 星期：1周一 ... 7周日 */
    private Integer weekday;
    /** 时段：1上午 2下午 */
    private Integer period;
    /** 该时段可约号源数 */
    private Integer maxCount;
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
