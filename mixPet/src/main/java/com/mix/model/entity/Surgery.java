package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 手术单
 */
@Data
public class Surgery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "surgery_id", type = IdType.AUTO)
    private Integer surgeryId;

    private Integer recordId;
    private Integer petId;
    private Integer clientId;
    /** 主刀医生id */
    private Integer doctorId;
    private String surgeryName;
    /** 类型：绝育/骨科/肿瘤/外伤/其他 */
    private String surgeryType;
    /** 麻醉方式：全身麻醉/局部麻醉/镇静 */
    private String anesthesiaType;
    private LocalDateTime scheduledTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String surgeon;
    private String assistant;
    /** 0待手术 1手术中 2已完成 3已取消 */
    private Integer status;
    /** 术前评估/准备 */
    private String preopNote;
    /** 术中记录 */
    private String intraopNote;
    /** 术后记录/医嘱 */
    private String postopNote;
    /** 手术费 */
    private BigDecimal fee;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Long isDel;
}
