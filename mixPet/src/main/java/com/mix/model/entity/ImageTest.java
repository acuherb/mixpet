package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 影像检查项目字典
 */
@Data
public class ImageTest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "item_id", type = IdType.AUTO)
    private Integer itemId;

    private String itemName;
    /** 分组（X光/B超/CT等） */
    private String groupName;
    /** 单价(元) */
    private BigDecimal price;
    private String detail;
    /** 报告模板项(JSON数组字符串，如 ["检查体位","影像所见"])，录报告时按项预置 */
    private String reportTpl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}