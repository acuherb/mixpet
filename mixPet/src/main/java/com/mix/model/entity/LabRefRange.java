package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 检验项目参考区间：按物种 / 性别 / 月龄分层，并带危急值阈值。
 * 开单时按宠物档案匹配后写入 lab_order_item 快照，字典后续修改不影响历史单据。
 */
@Data
@TableName("lab_ref_range")
public class LabRefRange implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "range_id", type = IdType.AUTO)
    private Integer rangeId;
    private Integer testId;
    /** 物种/品类，如 犬、猫；空=通用 */
    private String species;
    /** 性别，如 公、母；空=不限 */
    private String sex;
    /** 月龄区间（含端点），空=不限 */
    private Integer ageMinMonth;
    private Integer ageMaxMonth;
    private BigDecimal refLow;
    private BigDecimal refHigh;
    /** 危急值阈值：低于 criticalLow 或高于 criticalHigh 触发危急告警 */
    private BigDecimal criticalLow;
    private BigDecimal criticalHigh;
    /** 展示用文本，如 "6.0-17.0" 或 "阴性" */
    private String refText;
    private Boolean active;
}
