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
 * 药品出入库台账
 */
@Data
public class DrugRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "record_id", type = IdType.AUTO)
    private Integer recordId;

    private Integer drugId;
    /** 冗余药品名，便于台账展示 */
    private String drugName;
    /** 变动类型：1入库 2发药出库 3退款回库 4盘亏/报损 */
    private Integer changeType;
    /** 变动数量（正数） */
    private Integer quantity;
    /** 变动前库存 */
    private Integer beforeStock;
    /** 变动后库存 */
    private Integer afterStock;
    /** 操作人 */
    private String operator;
    /** 供应商id（入库时记） */
    private Integer supplierId;
    /** 冗余供应商名 */
    private String supplierName;
    /** 批号 */
    private String batchNo;
    /** 备注 */
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
