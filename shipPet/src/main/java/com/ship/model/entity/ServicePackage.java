package com.ship.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.List;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务套餐字典（驱虫/体检/免疫套餐等）
 */
@Data
public class ServicePackage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "package_id", type = IdType.AUTO)
    private Integer packageId;

    private String packageName;
    /** 分类：驱虫/体检/免疫/美容等 */
    private String category;
    /** 套餐价 */
    private BigDecimal price;
    /** 包含内容（文本描述，补充说明用） */
    private String content;
    /** 启停 */
    private Boolean active;
    /** 包含的检查项目（非数据库字段，保存时一并写入子表） */
    @TableField(exist = false)
    private List<PackageItem> items;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
