package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级类别折扣：某等级对某商品类别(drug_type)的折扣率，与等级折扣叠加。
 * 应用点：开方时按 drug.drug_type 查类别折扣率折进 prescription.drug_price（净价落库），
 * 下游账单生成列 / 退款 / 报表均按净价工作，无需感知折扣结构。
 */
@Data
@TableName("member_level_discount")
public class MemberLevelDiscount implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private Integer levelId;
    /** 商品类别（= sys_dict dict_type=drug_type 的值） */
    private String itemType;
    /** 类别折扣率（0-1，0.9=九折） */
    private BigDecimal discount;
    private Boolean active;
    private LocalDateTime updateTime;
}
