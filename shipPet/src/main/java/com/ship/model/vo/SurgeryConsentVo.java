package com.ship.model.vo;

import lombok.Data;

/**
 * 手术知情同意书签署请求
 */
@Data
public class SurgeryConsentVo {
    private Integer surgeryId;
    /** 风险与注意事项告知 */
    private String risks;
    /** 畜主是否同意 */
    private Boolean agree;
    /** 签字人（畜主姓名） */
    private String signer;
    private String note;
}
