package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 病历修改留痕：每次内容变更前保存整本快照，病历本身不再被静默覆盖。
 * 与 audit_log（操作摘要）互补：这里保留的是可用于追溯/复原的完整前后值。
 */
@Data
@TableName("medical_record_revision")
public class MedicalRecordRevision implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "revision_id", type = IdType.AUTO)
    private Long revisionId;
    private Integer recordId;
    /** 本次变更前病历的版本号（1=建档初始版） */
    private Integer revisionNo;
    /** 变更前整本病历 JSON 快照 */
    private String snapshotJson;
    private Integer editorId;
    private String editorName;
    /** 变更原因（修改/审核退回等） */
    private String changeReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
