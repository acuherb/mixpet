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
 * 手术/麻醉知情同意书：一手术一条
 */
@Data
public class SurgeryConsent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "consent_id", type = IdType.AUTO)
    private Integer consentId;

    private Integer surgeryId;
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    /** 冗余手术名/麻醉方式，便于展示 */
    private String surgeryName;
    private String anesthesiaType;
    /** 风险与注意事项告知 */
    private String risks;
    /** 畜主是否同意 */
    private Boolean agree;
    /** 签字人（畜主姓名） */
    private String signer;
    /** 签署时间 */
    private LocalDateTime signedTime;
    /** 经办医护 */
    private String operator;
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
