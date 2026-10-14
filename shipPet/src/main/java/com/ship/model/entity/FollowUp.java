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
 * 随访记录（术后/出院/慢病管理等）
 */
@Data
public class FollowUp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "follow_id", type = IdType.AUTO)
    private Integer followId;

    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    /** 随访方式：电话/微信/到店/上门 */
    private String followType;
    /** 随访内容/计划 */
    private String content;
    /** 随访结果/客户反馈 */
    private String result;
    /** 计划随访时间 */
    private LocalDateTime planTime;
    /** 实际随访时间 */
    private LocalDateTime followTime;
    /** 状态：false待随访 true已随访 */
    private Boolean status;
    /** 随访人 */
    private String followBy;
    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
