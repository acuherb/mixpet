package com.mix.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 寄养每日照片：文件名由服务端生成，读取时按 foster.client_id 做归属校验 */
@Data
@TableName("foster_photo")
public class FosterPhoto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "photo_id", type = IdType.AUTO)
    private Integer photoId;
    private Integer fosterId;
    private Integer logId;
    private LocalDate logDate;
    private String fileName;
    private String originName;
    private LocalDateTime createTime;
    private Long isDel;
}
