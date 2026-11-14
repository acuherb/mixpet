package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 寄养每日照护记录（文字），顾客按天查看 */
@Data
@TableName("foster_log")
public class FosterLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;
    private Integer fosterId;
    private LocalDate logDate;
    private String mentalState;
    private String appetite;
    private String defecation;
    private BigDecimal weight;
    private String note;
    private String recorder;
    private LocalDateTime createTime;
    private Long isDel;
}
