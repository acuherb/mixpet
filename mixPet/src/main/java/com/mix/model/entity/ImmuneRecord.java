package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宠物接种档案
 */
@Data
public class ImmuneRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "immune_id", type = IdType.AUTO)
    private Integer immuneId;

    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private Integer drugId;
    /** 冗余疫苗药品名 */
    private String drugName;
    /** 此针为第几针 */
    private Integer doseCnt;
    private LocalDateTime vaccinateTime;
    /** 下一针建议时间 */
    private LocalDateTime nextTime;
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}