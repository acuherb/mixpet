package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一次结构化体检。schemaJson / metaJson 均为当时的快照：
 * 管理员后续改模板或宠物改名，都不会改变已出具的历史报告。
 */
@Data
@TableName("checkup_record")
public class CheckupRecord implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "checkup_id", type = IdType.AUTO)
    private Integer checkupId;
    private Integer templateId;
    private String tplKey;
    private String templateName;
    /** 模板快照 */
    private String schemaJson;
    /** 逐项结果：key=item.key，值类型由 item.type 决定 */
    private String resultsJson;
    /** 表头快照：主人/宠物/医生/日期/体重等 */
    private String metaJson;
    private String conclusion;
    /** 关联病历（可为空：单独做的体检） */
    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    private Integer doctorId;
    private LocalDateTime checkTime;
    private String createBy;
    private LocalDateTime createTime;
    private Long isDel;
}
