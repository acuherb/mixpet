package com.mix.model.vo;

import lombok.Data;

/**
 * 影像开单请求
 */
@Data
public class ImageOrderVo {
    /** 病历id */
    private Integer recordId;
    /** 项目id */
    private Integer itemId;
    /** 检查部位/目的 */
    private String examPart;
    /** 备注 */
    private String note;
}