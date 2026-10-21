package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 化验单明细实体：项目名/分组/单位/参考范围在开单时冗余快照，不受字典后续修改影响
 */
@Data
public class LabOrderItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "item_id", type = IdType.AUTO)
    private Integer itemId;

    private Integer orderId;
    private Integer testId;
    /** 冗余：开单时项目名 */
    private String itemName;
    private String itemGroup;
    private String itemUnit;
    private String refRange;
    /** 检验结果值 */
    private String resultValue;
    /** 是否异常 */
    private Boolean isAbnormal;
    /** 出结果时间 */
    private LocalDateTime resultTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}