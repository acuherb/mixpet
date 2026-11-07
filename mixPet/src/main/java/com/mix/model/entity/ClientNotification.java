package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 顾客定向站内通知：报告出具、复诊提醒等，顾客只能看到自己名下的 */
@Data
@TableName("client_notification")
public class ClientNotification implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "notify_id", type = IdType.AUTO)
    private Long notifyId;
    private Integer clientId;
    private String title;
    private String content;
    /** REPORT_LAB / REPORT_IMAGE / GENERAL */
    private String notifyType;
    private String refType;
    private Integer refId;
    private Boolean isRead;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
