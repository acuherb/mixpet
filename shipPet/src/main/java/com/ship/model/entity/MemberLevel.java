package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级/会员卡模板：等级折扣、充值返赠、消费积分率
 */
@Data
public class MemberLevel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "level_id", type = IdType.AUTO)
    private Integer levelId;

    /** 等级键：0普通 1银 2金 3钻石（对应 client.client_level） */
    private Integer levelKey;
    private String levelName;
    /** 折扣率：1=不打折，0.95=95折 */
    private BigDecimal discount;
    /** 充值返赠比例：0.1=充100送10 */
    private BigDecimal rechargeBonus;
    /** 消费积分率：1元=N分 */
    private BigDecimal pointsRate;
    private Integer sort;
    private Boolean active;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
