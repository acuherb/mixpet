package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户次卡（购买后持有，可核销）
 */
@Data
public class ClientCountCard implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "card_id", type = IdType.AUTO)
    private Integer cardId;

    private Integer clientId;
    private Integer templateId;
    /** 冗余次卡名 */
    private String name;
    private Integer remainTimes;
    private Integer totalTimes;
    private LocalDate expireDate;
    /** true有效 false用完/作废 */
    private Boolean status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
