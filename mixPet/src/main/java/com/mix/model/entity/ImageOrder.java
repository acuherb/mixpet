package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 影像检查申请单
 */
@Data
public class ImageOrder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "order_id", type = IdType.AUTO)
    private Integer orderId;

    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private Integer itemId;
    /** 冗余项目名 */
    private String itemName;
    /** 冗余分组 */
    private String groupName;
    private BigDecimal fee;
    /** 检查部位/目的 */
    private String examPart;
    /** 影像报告内容 */
    private String result;
    /** 影像所见（客观描述）与影像诊断（结论）分离，避免一个文本框混写 */
    private String findings;
    private String impression;
    /** DICOM Study / Series UID：对接 PACS 时的检索键 */
    private String dicomUid;
    private String dicomSeries;
    /** 原图附件文件名 JSON 数组，如 ["a.dcm","b.png"] */
    private String imageFiles;
    private LocalDateTime reportTime;
    /** 0待检查 1已出报告 */
    private Boolean status;
    /** 报告文件(影像图/PDF)路径，可选 */
    private String reportFile;
    /** 是否作废 0否 1是 */
    private Boolean isCancel;
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}