package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 化验单实体：一张病历可开多张化验单，开单后由化验室录入结果
 */
@Data
public class LabOrder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "order_id", type = IdType.AUTO)
    private Integer orderId;

    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    /** 开单医生 */
    private Integer doctorId;
    /** 检验目的/备注 */
    private String orderNote;
    /** 化验费合计(元) */
    private BigDecimal fee;
    /** 0待检验 1已出结果 */
    private Boolean status;
    /** 化验小结/结论 */
    private String resultNote;
    /** 结果录入人 */
    private String resultBy;
    /** 报告文件(图片/PDF)路径，可选 */
    private String reportFile;
    /** 是否作废 0否 1是 */
    private Boolean isCancel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}