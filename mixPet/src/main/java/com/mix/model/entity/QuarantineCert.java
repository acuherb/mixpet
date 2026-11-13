package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检疫单：宠物出行/托运场景由医院开具的检疫证明。
 * 客户/宠物/医生信息开具时快照冗余，历史单据不受后续资料修改影响。
 * 版式参考 PetSystem ReportPris_jianyi.mrt。
 */
@Data
@TableName("quarantine_cert")
public class QuarantineCert implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "cert_id", type = IdType.AUTO)
    private Integer certId;

    /** 检疫单号 JY-YYYYMMDD-XXXX（唯一，占号不回收） */
    private String certNo;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    /** 开单人 */
    private Integer doctorId;

    /** 以下为开具时快照 */
    private String ownerName;
    private String ownerTel;
    private String petName;
    /** 动物种类（犬/猫…） */
    private String species;
    /** 品种 */
    private String breed;
    private String sex;
    /** 年龄描述，如 2岁3月 */
    private String ageDesc;
    private BigDecimal weight;

    /** 文字记录 / 检疫结论 */
    private String content;
    /** 检疫清单快照（JSON 数组：药品名/规格/数量/实际用量） */
    private String itemsJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Long isDel;
}
