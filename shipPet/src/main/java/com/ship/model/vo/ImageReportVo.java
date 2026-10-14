package com.ship.model.vo;

import lombok.Data;

/**
 * 影像报告录入请求
 */
@Data
public class ImageReportVo {
    /** 申请单id */
    private Integer orderId;
    /** 影像报告内容 */
    private String result;
    /** 报告文件(影像图/PDF)路径，可选 */
    private String reportFile;
}