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
 * 套餐包含的检查项目（检验/影像，快照）
 */
@Data
public class PackageItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer packageId;
    /** lab 检验 / image 影像 */
    private String itemType;
    private Integer itemId;
    /** 项目名快照 */
    private String itemName;
    /** 项目单价快照 */
    private BigDecimal itemPrice;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private Long isDel;
}
