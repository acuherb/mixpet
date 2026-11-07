package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 化验结果修订留痕：结果改动前的整单快照（含全部明细） */
@Data
@TableName("lab_result_revision")
public class LabResultRevision implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "revision_id", type = IdType.AUTO)
    private Long revisionId;
    private Integer orderId;
    private Integer revisionNo;
    private String snapshotJson;
    private String operator;
    private String changeReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
